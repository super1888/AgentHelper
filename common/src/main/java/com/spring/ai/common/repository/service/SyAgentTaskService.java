package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyAgentTask;

public interface SyAgentTaskService extends IService<SyAgentTask> {

    SyAgentTask getByTaskCode(String taskCode, Long tenantId);

    SyAgentTask getLatestFailedTask(Long sessionId, Long tenantId);

    SyAgentTask getRunningTask(Long sessionId, Long tenantId);
}
