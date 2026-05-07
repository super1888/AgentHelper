package com.spring.ai.user.application.service;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Service;

/**
 * SmartJavaAI 依赖缺失时的人脸服务降级实现。
 */
@Service
@ConditionalOnMissingClass({
        "cn.smartjavaai.common.enums.DeviceEnum",
        "cn.smartjavaai.face.factory.FaceDetModelFactory",
        "cn.smartjavaai.face.factory.FaceRecModelFactory"
})
public class UnavailableUserFaceRecognitionService implements UserFaceRecognitionService {

    private static final String UNAVAILABLE_MESSAGE =
            "人脸识别服务不可用，当前运行环境缺少 SmartJavaAI 依赖";

    /**
     * 依赖缺失时直接返回明确错误。
     */
    @Override
    public FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }

    /**
     * 依赖缺失时直接返回明确错误。
     */
    @Override
    public boolean isSameFace(String sourceEmbedding, String targetEmbedding) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }

    /**
     * 依赖缺失时直接返回明确错误。
     */
    @Override
    public int resolveEmbeddingDimension(String embedding) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }

    /**
     * 依赖缺失时返回默认版本标识。
     */
    @Override
    public String resolveEmbeddingVersion() {
        return "smartjavaai-unavailable";
    }
}
