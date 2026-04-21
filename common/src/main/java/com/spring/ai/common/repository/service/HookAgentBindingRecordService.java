package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.HookAgentBindingRecord;
import java.util.List;

/**
 * 文件用途：Hook Agent 绑定服务接口
 */
public interface HookAgentBindingRecordService extends IService<HookAgentBindingRecord> {

    /**
     * 查询 Hook 绑定列表。
     */
    List<HookAgentBindingRecord> listByHookId(Long hookId, Long tenantId);
}
