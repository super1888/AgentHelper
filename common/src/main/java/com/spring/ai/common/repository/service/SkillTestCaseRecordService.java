package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SkillTestCaseRecord;
import java.util.List;

public interface SkillTestCaseRecordService extends IService<SkillTestCaseRecord> {

    List<SkillTestCaseRecord> listBySkillId(Long skillId, Long tenantId);
}
