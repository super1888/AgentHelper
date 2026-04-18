package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SkillExecutionLogRecord;
import java.util.List;

public interface SkillExecutionLogRecordService extends IService<SkillExecutionLogRecord> {

    List<SkillExecutionLogRecord> listBySkillId(Long skillId, Long tenantId);

    List<SkillExecutionLogRecord> listByTenantId(Long tenantId);
}
