package com.spring.ai.skills.domain.dto;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.Resource;
import reactor.core.scheduler.Scheduler;

/**
 * SkillPromptAugmentAdvisor 创建入参 DTO * 技能提示词增强顾问（用于AI对话自动注入技能能力）
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */
@Data
@Builder
public class SkillPromptAugmentAdvisorDTO {

    // ===================== 核心路径参数 =====================
    /**
     * 用户级技能目录（字符串路径） 作用：存放用户自定义的技能文件目录 格式：文件系统绝对路径 / 相对路径
     */
    private String userSkillsDirectory;

    /**
     * 用户级技能目录（Spring Resource 对象） 作用：支持 classpath/file/URL 等资源形式传入 优先级：高于 userSkillsDirectory 字符串
     */
    private Resource userSkillsResource;

    /**
     * 项目级技能目录（字符串路径） 作用：存放项目内置默认技能文件目录
     */
    private String projectSkillsDirectory;

    /**
     * 项目级技能目录（Spring Resource 对象） 作用：资源形式传入项目技能目录 优先级：高于 projectSkillsDirectory 字符串
     */
    private Resource projectSkillsResource;

    // ===================== 依赖组件 =====================
    /**
     * 技能注册器 作用：已构建好的 SkillRegistry 实例，用于加载/管理技能
     */
    private SkillRegistry skillRegistry;

    // ===================== 执行控制参数 =====================
    /**
     * 执行顺序 作用：多个 Advisor 执行时的优先级，数字越小优先级越高
     */
    private Integer order;

    /**
     * 调度器 作用：异步加载/刷新技能的线程调度器
     */
    private Scheduler scheduler;

    /**
     * 是否懒加载 作用：true=使用时才加载技能；false=初始化立即加载 默认值：false
     */
    private Boolean lazyLoad;
}