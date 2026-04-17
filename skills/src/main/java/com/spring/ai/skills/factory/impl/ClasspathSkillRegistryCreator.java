package com.spring.ai.skills.factory.impl;

import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.spring.ai.common.enums.SkillFactoryTypeEnum;
import com.spring.ai.skills.domain.dto.ClasspathSkillRegistryDTO;
import com.spring.ai.skills.factory.SkillCreator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ClasspathSkillRegistry 创建器。
 */
@Component
public class ClasspathSkillRegistryCreator implements SkillCreator {

    @Override
    public SkillFactoryTypeEnum getSkillFactoryType() {
        return SkillFactoryTypeEnum.CLASSPATH_SKILL_REGISTRY;
    }

    @Override
    public Object create(Object dto) {
        ClasspathSkillRegistryDTO registryDTO = (ClasspathSkillRegistryDTO) dto;
        if (registryDTO == null) {
            throw new IllegalArgumentException("类路径技能注册表配置不能为空");
        }
        ClasspathSkillRegistry.Builder builder = new ClasspathSkillRegistry.Builder();
        if (StringUtils.hasText(registryDTO.getClasspathPath())) {
            builder.classpathPath(registryDTO.getClasspathPath());
        }
        if (StringUtils.hasText(registryDTO.getBasePath())) {
            builder.basePath(registryDTO.getBasePath());
        }
        if (registryDTO.getAutoLoad() != null) {
            builder.autoLoad(registryDTO.getAutoLoad());
        }
        if (registryDTO.getSystemPromptTemplate() != null) {
            builder.systemPromptTemplate(registryDTO.getSystemPromptTemplate());
        }
        if (StringUtils.hasText(registryDTO.getSystemPromptTemplateStr())) {
            builder.systemPromptTemplate(registryDTO.getSystemPromptTemplateStr());
        }
        return builder.build();
    }
}
