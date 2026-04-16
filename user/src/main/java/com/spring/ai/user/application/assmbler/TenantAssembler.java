package com.spring.ai.user.application.assmbler;

import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.user.domain.request.TenantCreateRequest;
import com.spring.ai.user.domain.request.TenantUpdateRequest;
import com.spring.ai.user.domain.vo.TenantOptionVO;
import com.spring.ai.user.domain.vo.TenantProfileVO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 租户模块对象转换器。
 */
public final class TenantAssembler {

    private TenantAssembler() {
    }

    /**
     * 将创建请求转换为租户实体。
     *
     * @param request 创建请求
     * @param ownerUserId 默认租户归属用户 ID
     * @param ownerUserName 默认租户归属用户名
     * @param isDefault 是否默认租户
     * @return 租户实体
     */
    public static SyTenant toCreateEntity(
            TenantCreateRequest request,
            Long ownerUserId,
            String ownerUserName,
            Integer isDefault
    ) {
        SyTenant tenant = new SyTenant();
        tenant.setTenantCode(trim(request.getTenantCode()));
        tenant.setTenantName(trim(request.getTenantName()));
        tenant.setStatus(request.getStatus());
        tenant.setIsDefault(isDefault);
        tenant.setOwnerUserId(ownerUserId);
        tenant.setOwnerUserName(trimToNull(ownerUserName));
        tenant.setContactName(trimToNull(request.getContactName()));
        tenant.setContactPhone(trimToNull(request.getContactPhone()));
        tenant.setDescription(trimToNull(request.getDescription()));
        return tenant;
    }

    /**
     * 合并更新请求到租户实体。
     *
     * @param tenant 租户实体
     * @param request 更新请求
     */
    public static void mergeForUpdate(SyTenant tenant, TenantUpdateRequest request) {
        tenant.setTenantCode(trim(request.getTenantCode()));
        tenant.setTenantName(trim(request.getTenantName()));
        tenant.setStatus(request.getStatus());
        tenant.setContactName(trimToNull(request.getContactName()));
        tenant.setContactPhone(trimToNull(request.getContactPhone()));
        tenant.setDescription(trimToNull(request.getDescription()));
    }

    /**
     * 转换为租户详情对象。
     *
     * @param tenant 租户实体
     * @param memberCount 成员数量
     * @return 展示对象
     */
    public static TenantProfileVO toTenantProfileVO(SyTenant tenant, Long memberCount) {
        if (tenant == null) {
            return null;
        }
        TenantProfileVO profileVO = new TenantProfileVO();
        profileVO.setId(tenant.getId());
        profileVO.setTenantCode(tenant.getTenantCode());
        profileVO.setTenantName(tenant.getTenantName());
        profileVO.setStatus(tenant.getStatus());
        profileVO.setIsDefault(tenant.getIsDefault());
        profileVO.setOwnerUserId(tenant.getOwnerUserId());
        profileVO.setOwnerUserName(tenant.getOwnerUserName());
        profileVO.setContactName(tenant.getContactName());
        profileVO.setContactPhone(tenant.getContactPhone());
        profileVO.setDescription(tenant.getDescription());
        profileVO.setMemberCount(memberCount);
        return profileVO;
    }

    /**
     * 转换为租户选项对象。
     *
     * @param tenant 租户实体
     * @return 选项对象
     */
    public static TenantOptionVO toTenantOptionVO(SyTenant tenant) {
        if (tenant == null) {
            return null;
        }
        TenantOptionVO optionVO = new TenantOptionVO();
        optionVO.setId(tenant.getId());
        optionVO.setTenantCode(tenant.getTenantCode());
        optionVO.setTenantName(tenant.getTenantName());
        optionVO.setStatus(tenant.getStatus());
        return optionVO;
    }

    /**
     * 转换租户选项列表。
     *
     * @param tenants 租户实体列表
     * @return 选项列表
     */
    public static List<TenantOptionVO> toTenantOptionVOList(List<SyTenant> tenants) {
        if (tenants == null || tenants.isEmpty()) {
            return Collections.emptyList();
        }
        return tenants.stream()
                .map(TenantAssembler::toTenantOptionVO)
                .collect(Collectors.toList());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
