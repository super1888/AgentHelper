package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.A2aExecutionLogRecord;
import java.util.List;

public interface A2aExecutionLogRecordService extends IService<A2aExecutionLogRecord> {

    List<A2aExecutionLogRecord> listByTenantId(Long tenantId);

    List<A2aExecutionLogRecord> listByTaskCode(Long tenantId, String taskCode);
}
