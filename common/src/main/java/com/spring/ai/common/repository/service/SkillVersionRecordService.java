package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import java.util.List;

public interface SkillVersionRecordService extends IService<SkillVersionRecord> {

    List<SkillVersionRecord> listBySkillId(Long skillId, Long tenantId);

    SkillVersionRecord getBySkillIdAndVersionNo(Long skillId, Long tenantId, Integer versionNo);
}
