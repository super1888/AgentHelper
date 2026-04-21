package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.HookVersionRecord;
import java.util.List;

/**
 * 文件用途：Hook 版本表服务接口
 */
public interface HookVersionRecordService extends IService<HookVersionRecord> {

    /**
     * 查询指定 Hook 的版本列表。
     */
    List<HookVersionRecord> listByHookId(Long hookId, Long tenantId);

    /**
     * 查询指定版本。
     */
    HookVersionRecord getByHookIdAndVersionNo(Long hookId, Long tenantId, Integer versionNo);
}
