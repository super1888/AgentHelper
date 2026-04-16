package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import java.util.List;

/**
 * 提示词模板台账服务
 */
public interface PromptTemplateRecordService extends IService<PromptTemplateRecord> {

    List<PromptTemplateRecord> listByTenantId(Long tenantId);

    PromptTemplateRecord getByTemplateCode(Long tenantId, String templateCode);
}
