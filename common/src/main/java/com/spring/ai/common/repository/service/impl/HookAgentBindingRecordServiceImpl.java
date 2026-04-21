package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.HookAgentBindingRecordMapper;
import com.spring.ai.common.repository.enitiy.HookAgentBindingRecord;
import com.spring.ai.common.repository.service.HookAgentBindingRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Hook Agent 绑定服务实现
 */
@Service
public class HookAgentBindingRecordServiceImpl extends ServiceImpl<HookAgentBindingRecordMapper, HookAgentBindingRecord>
        implements HookAgentBindingRecordService {

    @Override
    public List<HookAgentBindingRecord> listByHookId(Long hookId, Long tenantId) {
        return list(Wrappers.lambdaQuery(HookAgentBindingRecord.class)
                .eq(HookAgentBindingRecord::getTenantId, tenantId)
                .eq(HookAgentBindingRecord::getHookId, hookId)
                .orderByAsc(HookAgentBindingRecord::getPriorityNo)
                .orderByDesc(HookAgentBindingRecord::getId));
    }
}
