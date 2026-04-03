package com.spring.ai.skills.domain.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.Resource;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */
@Data
@Builder
public class FileSystemSkillRegistryDTO {

    /**
     * 用户级技能目录（字符串路径） 作用：存放用户自定义的技能文件，优先级高于项目目录 格式：绝对路径 / 相对路径，例如：/home/user/skills 或 ./user-skills
     */
    private String userSkillsDirectory;

    /**
     * 用户级技能目录（Spring Resource 对象） 作用：通过 Spring Resource 传入目录（支持文件、classpath、URL 等资源） 优先级：高于 userSkillsDirectory，有 Resource 则优先使用
     */
    private Resource userSkillsResource;

    /**
     * 项目级技能目录（字符串路径） 作用：存放项目内置的默认技能文件 格式：绝对路径 / 相对路径
     */
    private String projectSkillsDirectory;

    /**
     * 项目级技能目录（Spring Resource 对象） 作用：通过 Spring Resource 传入项目技能目录 优先级：高于 projectSkillsDirectory
     */
    private Resource projectSkillsResource;

    /**
     * 是否自动加载技能文件 作用：创建 Registry 后是否自动扫描目录并加载技能 默认：true（包装类 Boolean，方便判空）
     */
    private Boolean autoLoad;

    /**
     * 系统提示词模板（对象） 作用：AI 技能使用的系统提示词模板（完整对象） 优先级：高于字符串模板
     */
    private SystemPromptTemplate systemPromptTemplate;

    /**
     * 系统提示词模板（字符串） 作用：直接传入字符串模板，内部自动构建为 SystemPromptTemplate
     */
    private String systemPromptTemplateStr;
}
