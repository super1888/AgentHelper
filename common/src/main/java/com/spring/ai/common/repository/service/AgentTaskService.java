package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.AgentTask;

public interface AgentTaskService extends IService<AgentTask> {

    AgentTask getByTaskCode(String taskCode, Long tenantId);

    AgentTask getLatestFailedTask(Long sessionId, Long tenantId);

    AgentTask getRunningTask(Long sessionId, Long tenantId);
}
