package com.spring.ai.user.application.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.enitiy.SyUserFaceTemplate;
import com.spring.ai.common.repository.service.SyUserFaceTemplateService;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.common.security.ModelSecretCryptoService;
import com.spring.ai.common.support.TenantResolveSupport;
import com.spring.ai.common.utils.CommonDigestUtils;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import com.spring.ai.user.application.assmbler.UserAssembler;
import com.spring.ai.user.application.service.UserFaceRecognitionService;
import com.spring.ai.user.domain.request.UserFaceBindRequest;
import com.spring.ai.user.domain.request.UserFaceLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserFaceBindVO;
import com.spring.ai.user.domain.vo.UserFaceStatusVO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 人脸认证管理器。
 */
@Component
public class UserFaceAuthManager {

    @Resource
    private UserFaceRecognitionService userFaceRecognitionService;

    @Resource
    private SyUserService syUserService;

    @Resource
    private SyUserFaceTemplateService syUserFaceTemplateService;

    @Resource
    private ModelSecretCryptoService modelSecretCryptoService;

    @Resource
    private TenantResolveSupport tenantResolveSupport;

    /**
     * 绑定人脸模板。
     */
    public UserFaceBindVO bindFace(UserFaceBindRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        FaceLoginVerifyResponse verifyResponse = userFaceRecognitionService.verifyFace(buildVerifyRequest(
                request.getImageBase64(),
                request.getImageFormat(),
                request.getDeviceId(),
                request.getClientIp()
        ));
        SyUserFaceTemplate existed = syUserFaceTemplateService.getByUserId(userId);
        if (existed != null && !Boolean.TRUE.equals(request.getForceReplace())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "人脸已绑定，如需重新采集请传入 forceReplace=true");
        }

        SyUserFaceTemplate record = existed == null ? new SyUserFaceTemplate() : existed;
        record.setUserId(userId);
        record.setTenantId(tenantResolveSupport.resolveTenantIdByUserId(userId));
        record.setFaceTemplateCode("FACE_" + userId);
        record.setEmbeddingCipherText(modelSecretCryptoService.encrypt(verifyResponse.getFaceEmbedding()));
        record.setEmbeddingDimension(userFaceRecognitionService.resolveEmbeddingDimension(verifyResponse.getFaceEmbedding()));
        record.setEmbeddingVersion(userFaceRecognitionService.resolveEmbeddingVersion());
        record.setQualityScore(verifyResponse.getQualityScore());
        record.setLivenessScore(verifyResponse.getLivenessScore());
        record.setStatus("ENABLE");
        record.setImageSha256(CommonDigestUtils.sha256Hex(request.getImageBase64(), "人脸图片摘要生成失败"));
        record.setLastVerifiedTime(LocalDateTime.now());
        syUserFaceTemplateService.saveOrUpdate(record);

        UserFaceBindVO bindVO = new UserFaceBindVO();
        bindVO.setBound(true);
        bindVO.setQualityScore(record.getQualityScore());
        bindVO.setLivenessScore(record.getLivenessScore());
        bindVO.setFaceTemplateCode(record.getFaceTemplateCode());
        bindVO.setLastVerifiedTime(record.getLastVerifiedTime());
        return bindVO;
    }

    /**
     * 使用人脸登录。
     */
    public UserAuthLoginVO faceLogin(UserFaceLoginRequest request) {
        FaceLoginVerifyResponse verifyResponse = userFaceRecognitionService.verifyFace(buildVerifyRequest(
                request.getImageBase64(),
                request.getImageFormat(),
                request.getDeviceId(),
                request.getClientIp()
        ));
        SyUserFaceTemplate matchedRecord = syUserFaceTemplateService.lambdaQuery()
                .eq(SyUserFaceTemplate::getStatus, "ENABLE")
                .list()
                .stream()
                .filter(item -> userFaceRecognitionService.isSameFace(
                        modelSecretCryptoService.decrypt(item.getEmbeddingCipherText()),
                        verifyResponse.getFaceEmbedding()
                ))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCodeEnum.UNAUTHORIZED,
                        HttpStatus.UNAUTHORIZED,
                        "未找到匹配的人脸模板"
                ));

        SyUser user = syUserService.getDetailById(matchedRecord.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "匹配的用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.USER_DISABLED, HttpStatus.FORBIDDEN, "用户已停用");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set(UserAuthConstants.LOGIN_NAME, user.getUsername());
        matchedRecord.setLastVerifiedTime(LocalDateTime.now());
        syUserFaceTemplateService.updateById(matchedRecord);

        UserAuthLoginVO loginVO = new UserAuthLoginVO();
        loginVO.setUser(UserAssembler.toUserProfileVO(user, tenantResolveSupport.resolveTenantName(user.getTenantId())));
        loginVO.setToken(UserAssembler.toUserTokenVO(user.getId()));
        return loginVO;
    }

    /**
     * 获取当前账号的人脸状态。
     */
    public UserFaceStatusVO faceStatus() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SyUserFaceTemplate record = syUserFaceTemplateService.getByUserId(userId);
        UserFaceStatusVO statusVO = new UserFaceStatusVO();
        statusVO.setBound(record != null);
        statusVO.setFaceTemplateCode(record == null ? null : record.getFaceTemplateCode());
        statusVO.setStatus(record == null ? "UNBOUND" : record.getStatus());
        statusVO.setLastVerifiedTime(record == null ? null : record.getLastVerifiedTime());
        return statusVO;
    }

    /**
     * 解绑当前账号的人脸模板。
     */
    public void unbindFace() {
        StpUtil.checkLogin();
        SyUserFaceTemplate record = syUserFaceTemplateService.getByUserId(StpUtil.getLoginIdAsLong());
        if (record != null) {
            syUserFaceTemplateService.removeById(record.getId());
        }
    }

    private FaceLoginVerifyRequest buildVerifyRequest(String imageBase64, String imageFormat, String deviceId, String clientIp) {
        if (!StringUtils.hasText(imageBase64) || !StringUtils.hasText(imageFormat)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片内容和格式不能为空");
        }
        FaceLoginVerifyRequest verifyRequest = new FaceLoginVerifyRequest();
        verifyRequest.setImageBase64(imageBase64);
        verifyRequest.setImageFormat(imageFormat);
        verifyRequest.setDeviceId(deviceId);
        verifyRequest.setClientIp(clientIp);
        return verifyRequest;
    }
}
