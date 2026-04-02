package com.spring.ai.hooks.domain.dto;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;

/**
 * SkillsAgentHook 技能代理钩子DTO
 * 用于承载技能代理相关配置、技能注册器、工具回调分组等核心参数
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/2
 */
@Data
@Builder
public class SkillsAgentHookDTO {

    /**
     * 技能注册器
     * 负责管理、注册、查询系统内所有技能实例，是技能体系的核心注册中心
     */
    private SkillRegistry skillRegistry;

    /**
     * 是否自动重载
     * true：技能/工具配置变更时自动重新加载；false：需手动触发重载
     */
    private Boolean autoReload;

    /**
     * 分组工具回调集合
     * Key：工具分组名称（如：file、network、ai等）
     * Value：对应分组下的所有工具回调执行器列表
     */
    private Map<String, List<ToolCallback>> groupedTools;

}
