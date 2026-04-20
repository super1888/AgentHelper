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

    /**
     * 返回当前创建器负责的模板类型。
     */
    @Override
    public PromptTemplateTypeEnum getPromptTemplateType() {
        return PromptTemplateTypeEnum.SYSTEM_PROMPT_TEMPLATE;
    }

    /**
     * 根据 DTO 中的模板文本、资源和变量配置构建系统提示词模板。
     */
    @Override
    public Object create(Object dto) {
        SystemPromptTemplateDTO templateDTO = (SystemPromptTemplateDTO) dto;
        if (templateDTO == null) {
            throw new IllegalArgumentException("系统提示词模板配置不能为空");
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
