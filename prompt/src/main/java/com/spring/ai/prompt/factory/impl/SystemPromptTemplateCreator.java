package com.spring.ai.prompt.factory.impl;

import com.spring.ai.common.enums.PromptTemplateTypeEnum;
import com.spring.ai.prompt.domain.dto.SystemPromptTemplateDTO;
import com.spring.ai.prompt.factory.PromptTemplateCreator;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SystemPromptTemplate 创建器。
 */
@Component
public class SystemPromptTemplateCreator implements PromptTemplateCreator {

    @Override
    public PromptTemplateTypeEnum getPromptTemplateType() {
        return PromptTemplateTypeEnum.SYSTEM_PROMPT_TEMPLATE;
    }

    @Override
    public Object create(Object dto) {
        SystemPromptTemplateDTO templateDTO = (SystemPromptTemplateDTO) dto;
        if (templateDTO == null) {
            throw new IllegalArgumentException("SystemPromptTemplateDTO 不能为空");
        }

        SystemPromptTemplate.Builder builder = new SystemPromptTemplate.Builder();
        if (StringUtils.hasText(templateDTO.getTemplate())) {
            builder.template(templateDTO.getTemplate());
        }
        if (templateDTO.getResource() != null) {
            builder.resource(templateDTO.getResource());
        }
        if (templateDTO.getVariables() != null) {
            builder.variables(templateDTO.getVariables());
        }
        if (templateDTO.getRenderer() != null) {
            builder.renderer(templateDTO.getRenderer());
        }
        return builder.build();
    }
}
