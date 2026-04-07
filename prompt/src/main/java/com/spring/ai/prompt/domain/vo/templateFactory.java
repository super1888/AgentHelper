package com.spring.ai.prompt.domain.vo;

import com.spring.ai.prompt.domain.dto.SystemPromptTemplateDTO;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.StringUtils;

/**
 * 模板工厂
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */
public class templateFactory {

    /**
     * 创建系统提示词模板 SystemPromptTemplate
     *
     * @param dto 入参DTO
     * @return SystemPromptTemplate 实例
     */
    public SystemPromptTemplate createSystemPromptTemplate(SystemPromptTemplateDTO dto) {
        // 1. DTO 本身判空
        if (dto == null) {
            throw new IllegalArgumentException("创建参数 SystemPromptTemplateDTO 不能为空");
        }

        // 获取 Builder
        SystemPromptTemplate.Builder builder = new SystemPromptTemplate.Builder();

        // 2. 设置模板字符串（有值才设置）
        if (StringUtils.hasText(dto.getTemplate())) {
            builder.template(dto.getTemplate());
        }

        // 3. 设置模板资源文件（有值才设置）
        if (dto.getResource() != null) {
            builder.resource(dto.getResource());
        }

        // 4. 设置模板变量（Map 不为 null 才设置）
        if (dto.getVariables() != null) {
            builder.variables(dto.getVariables());
        }

        // 5. 设置自定义渲染器（有值才设置）
        if (dto.getRenderer() != null) {
            builder.renderer(dto.getRenderer());
        }

        // 构建并返回
        return builder.build();
    }

}
