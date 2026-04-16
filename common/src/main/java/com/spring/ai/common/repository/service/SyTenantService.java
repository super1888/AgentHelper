package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyTenant;
import java.util.List;

/**
 * 租户仓储服务接口。
 */
public interface SyTenantService extends IService<SyTenant> {

    /**
     * 按租户编码查询租户。
     *
     * @param tenantCode 租户编码
     * @return 租户实体
     */
    SyTenant getByTenantCode(String tenantCode);

    /**
     * 按主键查询租户详情。
     *
     * @param tenantId 租户 ID
     * @return 租户实体
     */
    SyTenant getDetailById(Long tenantId);

    /**
     * 查询默认租户。
     *
     * @param ownerUserId 归属用户 ID
     * @return 默认租户
     */
    SyTenant getDefaultTenantByOwnerUserId(Long ownerUserId);

    /**
     * 查询全部租户。
     *
     * @return 租户列表
     */
    List<SyTenant> listAllTenants();

    /**
     * 按条件分页查询租户。
     *
     * @param tenantName 租户名称
     * @param tenantCode 租户编码
     * @param status 租户状态
     * @return 租户列表
     */
    List<SyTenant> pageQueryTenants(String tenantName, String tenantCode, Integer status);

    /**
     * 统计全部租户数。
     *
     * @return 租户总数
     */
    long countAllTenants();

    /**
     * 按状态统计租户数。
     *
     * @param status 状态
     * @return 租户数量
     */
    long countByStatus(Integer status);
}
