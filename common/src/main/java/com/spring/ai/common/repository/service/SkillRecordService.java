package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import java.util.List;

public interface SkillRecordService extends IService<SkillRecord> {

    List<SkillRecord> listByTenantId(Long tenantId);

    List<SkillRecord> listDeletedByTenantId(Long tenantId);

    SkillRecord getBySkillCode(Long tenantId, String skillCode);
}
