package com.spring.ai.skills.factory;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.spring.ai.skills.domain.dto.ClasspathSkillRegistryDTO;
import com.spring.ai.skills.domain.dto.FileSystemSkillRegistryDTO;
import org.springframework.util.StringUtils;

/**
 * 注册skills 登记表
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */
public class RegistryFactory {

    /**
     * 构建ClasspathSkillRegistry对象
     *
     * @param dto
     * @return
     */
    public SkillRegistry creatClasspathSkillRegistry(ClasspathSkillRegistryDTO dto) {
        // 1. DTO 本身判空
        if (dto == null) {
            throw new IllegalArgumentException("创建参数ClasspathSkillRegistryDTO 不能为空");
        }
        ClasspathSkillRegistry.Builder builder = new ClasspathSkillRegistry.Builder();
        // 2. 路径类必填校验（不能为null、不能空白）
        if (StringUtils.hasText(dto.getClasspathPath())) {
            builder.classpathPath(dto.getClasspathPath());
        }
        if (StringUtils.hasText(dto.getBasePath())) {
            builder.basePath(dto.getBasePath());
        }

        if (dto.getAutoLoad() != null) {
            builder.autoLoad(dto.getAutoLoad());
        }

        if (dto.getSystemPromptTemplate() != null) {
            builder.systemPromptTemplate(dto.getSystemPromptTemplate());
        }

        if (StringUtils.hasText(dto.getSystemPromptTemplateStr())) {
            builder.systemPromptTemplate(dto.getSystemPromptTemplateStr());
        }

        return builder.build();
    }

    /**
     * 创建 文件系统技能注册器 FileSystemSkillRegistry
     * @param dto 入参DTO
     * @return SkillRegistry 实例
     */
    public SkillRegistry creatFileSystemSkillRegistry(FileSystemSkillRegistryDTO dto) {
        // 1. DTO 不能为空判断
        if (dto == null) {
            throw new IllegalArgumentException("创建参数 FileSystemSkillRegistryDTO 不能为空");
        }

        // 初始化 Builder
        FileSystemSkillRegistry.Builder builder = new FileSystemSkillRegistry.Builder();

        // 2. 设置用户技能目录
        // 优先使用 Resource 对象，不存在则使用字符串路径
        if (dto.getUserSkillsResource() != null) {
            builder.userSkillsDirectory(dto.getUserSkillsResource());
        } else if (StringUtils.hasText(dto.getUserSkillsDirectory())) {
            builder.userSkillsDirectory(dto.getUserSkillsDirectory());
        }

        // 3. 设置项目技能目录
        // 优先使用 Resource 对象，不存在则使用字符串路径
        if (dto.getProjectSkillsResource() != null) {
            builder.projectSkillsDirectory(dto.getProjectSkillsResource());
        } else if (StringUtils.hasText(dto.getProjectSkillsDirectory())) {
            builder.projectSkillsDirectory(dto.getProjectSkillsDirectory());
        }

        // 4. 设置是否自动加载技能文件
        // 传入不为 null 时才覆盖，否则使用 Builder 默认值 true
        if (dto.getAutoLoad() != null) {
            builder.autoLoad(dto.getAutoLoad());
        }

        // 5. 设置系统提示词模板（对象）
        if (dto.getSystemPromptTemplate() != null) {
            builder.systemPromptTemplate(dto.getSystemPromptTemplate());
        }

        // 6. 设置系统提示词模板（字符串）
        // 字符串有内容时才设置
        if (StringUtils.hasText(dto.getSystemPromptTemplateStr())) {
            builder.systemPromptTemplate(dto.getSystemPromptTemplateStr());
        }

        // 构建并返回最终实例
        return builder.build();
    }

}
