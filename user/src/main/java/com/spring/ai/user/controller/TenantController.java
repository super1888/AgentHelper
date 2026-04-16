package com.spring.ai.user.controller;

import com.github.pagehelper.PageInfo;
import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.TenantApplicationManager;
import com.spring.ai.user.domain.request.TenantCreateRequest;
import com.spring.ai.user.domain.request.TenantPageQueryRequest;
import com.spring.ai.user.domain.request.TenantUpdateRequest;
import com.spring.ai.user.domain.vo.TenantOptionVO;
import com.spring.ai.user.domain.vo.TenantProfileVO;
import com.spring.ai.user.domain.vo.TenantStatisticsVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户中心控制器。
 */
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantApplicationManager tenantApplicationManager;

    /**
     * 创建租户。
     *
     * @param request 创建请求
     * @return 空响应
     */
    @PostMapping("/add")
    public ApiResponse<Void> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        tenantApplicationManager.createTenant(request);
        return ApiResponse.success("新增租户成功", null);
    }

    /**
     * 更新租户。
     *
     * @param tenantId 租户 ID
     * @param request 更新请求
     * @return 空响应
     */
    @PutMapping("/update/{tenantId}")
    public ApiResponse<Void> updateTenant(@PathVariable Long tenantId, @Valid @RequestBody TenantUpdateRequest request) {
        tenantApplicationManager.updateTenant(tenantId, request);
        return ApiResponse.success("编辑租户成功", null);
    }

    /**
     * 删除租户。
     *
     * @param tenantId 租户 ID
     * @return 空响应
     */
    @DeleteMapping("/delete/{tenantId}")
    public ApiResponse<Void> deleteTenant(@PathVariable Long tenantId) {
        tenantApplicationManager.deleteTenant(tenantId);
        return ApiResponse.success("删除租户成功", null);
    }

    /**
     * 查询租户详情。
     *
     * @param tenantId 租户 ID
     * @return 租户详情
     */
    @GetMapping("/select/{tenantId}")
    public ApiResponse<TenantProfileVO> getTenantDetail(@PathVariable Long tenantId) {
        return ApiResponse.success(tenantApplicationManager.getTenantDetail(tenantId));
    }

    /**
     * 分页查询租户。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/pageQuery")
    public ApiResponse<PageInfo<TenantProfileVO>> pageQuery(@RequestBody(required = false) TenantPageQueryRequest request) {
        return ApiResponse.success(tenantApplicationManager.pageQueryTenants(
                request == null ? new TenantPageQueryRequest() : request));
    }

    /**
     * 查询租户选项。
     *
     * @return 选项列表
     */
    @GetMapping("/options")
    public ApiResponse<List<TenantOptionVO>> listTenantOptions() {
        return ApiResponse.success(tenantApplicationManager.listTenantOptions());
    }

    /**
     * 租户统计。
     *
     * @return 统计结果
     */
    @PostMapping("/statistics")
    public ApiResponse<TenantStatisticsVO> statistics() {
        return ApiResponse.success(tenantApplicationManager.tenantStatistics());
    }
}
