package com.spring.ai.hooks.factory.impl;

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.SkillsAgentHookDTO;
import com.spring.ai.hooks.factory.AbstractHookCreator;
import org.springframework.stereotype.Component;

/**
 * Skills Hook 创建器。
 */
@Component
public class SkillsAgentHookCreator extends AbstractHookCreator {

    @Override
    public HookTypeEnum getHookType() {
        return HookTypeEnum.SKILLS;
    }

    @Override
    public Object create(Object dto) {
        SkillsAgentHookDTO hookDTO = (SkillsAgentHookDTO) dto;
        if (hookDTO == null) {
            throw new IllegalArgumentException("技能智能体钩子配置不能为空");
        }

        SkillsAgentHook.Builder builder = SkillsAgentHook.builder();
        if (hookDTO.getSkillRegistry() != null) {
            builder.skillRegistry(hookDTO.getSkillRegistry());
        }
        builder.autoReload(hookDTO.getAutoReload());
        if (hookDTO.getGroupedTools() != null) {
            builder.groupedTools(hookDTO.getGroupedTools());
        }
        return builder.build();
    }
}
