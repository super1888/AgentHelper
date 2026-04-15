package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.common.repository.dao.SyAgentTaskMapper;
import com.spring.ai.common.repository.enitiy.SyAgentTask;
import com.spring.ai.common.repository.service.SyAgentTaskService;
import com.spring.ai.common.constants.SqlConstants;
import org.springframework.stereotype.Service;

@Service
public class SyAgentTaskServiceImpl extends ServiceImpl<SyAgentTaskMapper, SyAgentTask> implements SyAgentTaskService {

    @Override
    public SyAgentTask getByTaskCode(String taskCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(SyAgentTask.class)
                .eq(SyAgentTask::getTaskCode, taskCode)
                .eq(SyAgentTask::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyAgentTask getLatestFailedTask(Long sessionId, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(SyAgentTask.class)
                .eq(SyAgentTask::getSessionId, sessionId)
                .eq(SyAgentTask::getTenantId, tenantId)
                .eq(SyAgentTask::getTaskStatus, SimpleAgentConstants.TASK_STATUS_FAILED)
                .orderByDesc(SyAgentTask::getUpdateTime)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyAgentTask getRunningTask(Long sessionId, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(SyAgentTask.class)
                .eq(SyAgentTask::getSessionId, sessionId)
                .eq(SyAgentTask::getTenantId, tenantId)
                .eq(SyAgentTask::getTaskStatus, SimpleAgentConstants.TASK_STATUS_RUNNING)
                .last(SqlConstants.LIMIT_ONE));
    }
}
