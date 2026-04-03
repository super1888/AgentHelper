package com.spring.ai.skills.domain.dto;

/**
 * ClasspathSkillRegistry 创建入参 DTO
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

@Data
@Builder
public class ClasspathSkillRegistryDTO {

    /**
     * 类路径
     */
    private String classpathPath;

    /**
     * 基础路径
     */
    private String basePath;

    /**
     * 是否自动加载（默认 true）
     */
    private Boolean autoLoad;

    /**
     * 系统提示词模板（对象）
     */
    private SystemPromptTemplate systemPromptTemplate;

    /**
     * 系统提示词模板（字符串，二选一）
     */
    private String systemPromptTemplateStr;
}
