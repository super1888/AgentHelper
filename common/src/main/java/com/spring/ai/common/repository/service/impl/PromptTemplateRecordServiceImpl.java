package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.PromptTemplateRecordMapper;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.repository.service.PromptTemplateRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 提示词模板台账服务实现
 */
@Service
public class PromptTemplateRecordServiceImpl extends ServiceImpl<PromptTemplateRecordMapper, PromptTemplateRecord>
        implements PromptTemplateRecordService {

    @Override
    public List<PromptTemplateRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(PromptTemplateRecord.class)
                .eq(PromptTemplateRecord::getTenantId, tenantId)
                .orderByDesc(PromptTemplateRecord::getUpdateTime)
                .orderByDesc(PromptTemplateRecord::getId));
    }

    @Override
    public PromptTemplateRecord getByTemplateCode(Long tenantId, String templateCode) {
        if (tenantId == null || !StringUtils.hasText(templateCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(PromptTemplateRecord.class)
                .eq(PromptTemplateRecord::getTenantId, tenantId)
                .eq(PromptTemplateRecord::getTemplateCode, templateCode.trim())
                .last(SqlConstants.LIMIT_ONE));
    }
}
