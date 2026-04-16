package com.spring.ai.user.application.manager;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.TenantAssembler;
import com.spring.ai.user.domain.request.TenantCreateRequest;
import com.spring.ai.user.domain.request.TenantPageQueryRequest;
import com.spring.ai.user.domain.request.TenantUpdateRequest;
import com.spring.ai.user.domain.vo.TenantOptionVO;
import com.spring.ai.user.domain.vo.TenantProfileVO;
import com.spring.ai.user.domain.vo.TenantStatisticsVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 租户中心应用管理器。
 */
@Component
public class TenantApplicationManager {

    @Resource
    private SyTenantService syTenantService;

    @Resource
    private AgentService agentService;

    @Resource
    private SyUserService syUserService;

    /**
     * 创建租户。
     *
     * @param request 创建请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTenant(TenantCreateRequest request) {
        validateTenantRequest(request.getTenantCode(), request.getTenantName(), request.getStatus());
        validateTenantCodeUnique(null, request.getTenantCode());

        SyTenant tenant = TenantAssembler.toCreateEntity(request, null, null, 0);
        syTenantService.save(tenant);
    }

    /**
     * 更新租户。
     *
     * @param tenantId 租户 ID
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(Long tenantId, TenantUpdateRequest request) {
        validateTenantRequest(request.getTenantCode(), request.getTenantName(), request.getStatus());
        SyTenant tenant = requireTenant(tenantId);
        validateTenantCodeUnique(tenantId, request.getTenantCode());
        TenantAssembler.mergeForUpdate(tenant, request);
        syTenantService.updateById(tenant);
    }

    /**
     * 删除租户。
     *
     * @param tenantId 租户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(Long tenantId) {
        SyTenant tenant = requireTenant(tenantId);
        if (tenant.getIsDefault() != null && tenant.getIsDefault() == 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "默认租户不允许删除");
        }
        // 租户仍被业务数据占用时禁止删除，避免留下用户或 Agent 的悬挂归属。
        if (syUserService.countByTenantId(tenantId) > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前租户下仍存在用户，不能删除");
        }
        if (agentService.countByTenantId(tenantId) > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前租户下仍存在 Agent，不能删除");
        }
        syTenantService.removeById(tenantId);
    }

    /**
     * 查询租户详情。
     *
     * @param tenantId 租户 ID
     * @return 租户详情
     */
    public TenantProfileVO getTenantDetail(Long tenantId) {
        SyTenant tenant = requireTenant(tenantId);
        return TenantAssembler.toTenantProfileVO(tenant, syUserService.countByTenantId(tenantId));
    }

    /**
     * 分页查询租户。
     *
     * @param request 分页请求
     * @return 分页结果
     */
    public PageInfo<TenantProfileVO> pageQueryTenants(TenantPageQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SyTenant> tenants = syTenantService.pageQueryTenants(
                normalize(request.getTenantName()),
                normalize(request.getTenantCode()),
                normalizeStatus(request.getStatus())
        );

        PageInfo<SyTenant> sourcePageInfo = new PageInfo<>(tenants);
        PageInfo<TenantProfileVO> targetPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(sourcePageInfo, targetPageInfo, "list");
        targetPageInfo.setList(sourcePageInfo.getList().stream()
                .map(tenant -> TenantAssembler.toTenantProfileVO(tenant, syUserService.countByTenantId(tenant.getId())))
                .toList());
        return targetPageInfo;
    }

    /**
     * 查询租户选项。
     *
     * @return 租户选项列表
     */
    public List<TenantOptionVO> listTenantOptions() {
        return TenantAssembler.toTenantOptionVOList(
                syTenantService.listAllTenants().stream()
                        .filter(tenant -> UserStatusEnum.ENABLE.getCode().equals(tenant.getStatus()))
                        .toList()
        );
    }

    /**
     * 租户统计。
     *
     * @return 统计结果
     */
    public TenantStatisticsVO tenantStatistics() {
        TenantStatisticsVO statisticsVO = new TenantStatisticsVO();
        statisticsVO.setTotalCount(syTenantService.countAllTenants());
        statisticsVO.setEnabledCount(syTenantService.countByStatus(UserStatusEnum.ENABLE.getCode()));
        statisticsVO.setDisabledCount(syTenantService.countByStatus(UserStatusEnum.DISABLED.getCode()));
        return statisticsVO;
    }

    /**
     * 按租户 ID 校验并返回租户。
     *
     * @param tenantId 租户 ID
     * @return 租户实体
     */
    public SyTenant requireTenant(Long tenantId) {
        SyTenant tenant = syTenantService.getDetailById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    /**
     * 为用户初始化默认租户。
     *
     * <p>当前项目支持真正的租户中心后，如果用户未显式分配租户，
     * 就为其创建一条默认租户记录，再将用户绑定到该租户。</p>
     *
     * @param user 用户实体
     * @return 默认租户实体
     */
    @Transactional(rollbackFor = Exception.class)
    public SyTenant initializeDefaultTenant(SyUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "用户不能为空");
        }

        if (user.getTenantId() != null) {
            SyTenant existingTenant = syTenantService.getDetailById(user.getTenantId());
            if (existingTenant != null) {
                return existingTenant;
            }
        }

        SyTenant defaultTenant = syTenantService.getDefaultTenantByOwnerUserId(user.getId());
        if (defaultTenant == null) {
            TenantCreateRequest request = new TenantCreateRequest();
            request.setTenantCode(buildDefaultTenantCode(user));
            request.setTenantName(buildDefaultTenantName(user));
            request.setStatus(UserStatusEnum.ENABLE.getCode());
            request.setContactName(user.getNickname());
            request.setContactPhone(user.getPhone());
            defaultTenant = TenantAssembler.toCreateEntity(request, user.getId(), user.getUsername(), 1);
            syTenantService.save(defaultTenant);
        }

        user.setTenantId(defaultTenant.getId());
        syUserService.updateById(user);
        return defaultTenant;
    }

    private void validateTenantRequest(String tenantCode, String tenantName, Integer status) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "tenantCode must not be blank");
        }
        if (!StringUtils.hasText(tenantName)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "tenantName must not be blank");
        }
        if (status == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "status must not be null");
        }
        UserStatusEnum.fromVal(status);
    }

    private void validateTenantCodeUnique(Long currentTenantId, String tenantCode) {
        SyTenant tenant = syTenantService.getByTenantCode(normalize(tenantCode));
        if (tenant != null && !tenant.getId().equals(currentTenantId)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.CONFLICT, "租户编码已存在");
        }
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        UserStatusEnum.fromVal(status);
        return status;
    }

    private String buildDefaultTenantCode(SyUser user) {
        return "DEFAULT_" + user.getId();
    }

    private String buildDefaultTenantName(SyUser user) {
        String baseName = StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
        return baseName + "默认租户";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
