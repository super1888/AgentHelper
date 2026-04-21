package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.HookRecord;
import java.util.List;

/**
 * 文件用途：Hook 主表服务接口
 */
public interface HookRecordService extends IService<HookRecord> {

    /**
     * 查询租户下有效 Hook 列表。
     */
    List<HookRecord> listByTenantId(Long tenantId);

    /**
     * 查询租户下已删除 Hook 列表。
     */
    List<HookRecord> listDeletedByTenantId(Long tenantId);

    /**
     * 按 Hook 编码查询记录。
     */
    HookRecord getByHookCode(Long tenantId, String hookCode);
}
