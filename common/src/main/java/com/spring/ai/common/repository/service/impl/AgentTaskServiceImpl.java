package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.common.repository.dao.AgentTaskMapper;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.service.AgentTaskService;
import com.spring.ai.common.constants.SqlConstants;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskServiceImpl extends ServiceImpl<AgentTaskMapper, AgentTask> implements AgentTaskService {

    @Override
    public AgentTask getByTaskCode(String taskCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(AgentTask.class)
                .eq(AgentTask::getTaskCode, taskCode)
                .eq(AgentTask::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public AgentTask getLatestFailedTask(Long sessionId, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(AgentTask.class)
                .eq(AgentTask::getSessionId, sessionId)
                .eq(AgentTask::getTenantId, tenantId)
                .eq(AgentTask::getTaskStatus, SimpleAgentConstants.TASK_STATUS_FAILED)
                .orderByDesc(AgentTask::getUpdateTime)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public AgentTask getRunningTask(Long sessionId, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(AgentTask.class)
                .eq(AgentTask::getSessionId, sessionId)
                .eq(AgentTask::getTenantId, tenantId)
                .eq(AgentTask::getTaskStatus, SimpleAgentConstants.TASK_STATUS_RUNNING)
                .last(SqlConstants.LIMIT_ONE));
    }
}
