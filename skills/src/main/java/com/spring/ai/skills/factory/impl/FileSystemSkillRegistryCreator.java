package com.spring.ai.skills.factory.impl;

import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.spring.ai.common.enums.SkillFactoryTypeEnum;
import com.spring.ai.skills.domain.dto.FileSystemSkillRegistryDTO;
import com.spring.ai.skills.factory.SkillCreator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * FileSystemSkillRegistry 创建器。
 */
@Component
public class FileSystemSkillRegistryCreator implements SkillCreator {

    @Override
    public SkillFactoryTypeEnum getSkillFactoryType() {
        return SkillFactoryTypeEnum.FILE_SYSTEM_SKILL_REGISTRY;
    }

    @Override
    public Object create(Object dto) {
        FileSystemSkillRegistryDTO registryDTO = (FileSystemSkillRegistryDTO) dto;
        if (registryDTO == null) {
            throw new IllegalArgumentException("文件系统技能注册表配置不能为空");
        }

        FileSystemSkillRegistry.Builder builder = new FileSystemSkillRegistry.Builder();
        if (registryDTO.getUserSkillsResource() != null) {
            builder.userSkillsDirectory(registryDTO.getUserSkillsResource());
        }
        else if (StringUtils.hasText(registryDTO.getUserSkillsDirectory())) {
            builder.userSkillsDirectory(registryDTO.getUserSkillsDirectory());
        }
        if (registryDTO.getProjectSkillsResource() != null) {
            builder.projectSkillsDirectory(registryDTO.getProjectSkillsResource());
        }
        else if (StringUtils.hasText(registryDTO.getProjectSkillsDirectory())) {
            builder.projectSkillsDirectory(registryDTO.getProjectSkillsDirectory());
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
