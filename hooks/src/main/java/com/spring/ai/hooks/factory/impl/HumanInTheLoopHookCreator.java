package com.spring.ai.hooks.factory.impl;

import static com.spring.ai.common.utils.BaseUtils.getOrDefault;

import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.HumanInTheLoopHookDTO;
import com.spring.ai.hooks.factory.AbstractHookCreator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 人工审批 Hook 创建器。
 * @author zhuoqi
 */
@Component
public class HumanInTheLoopHookCreator extends AbstractHookCreator {

    @Override
    public HookTypeEnum getHookType() {
        return HookTypeEnum.HUMAN_IN_THE_LOOP;
    }

    @Override
    public Object create(Object dto) {
        HumanInTheLoopHookDTO hookDTO = (HumanInTheLoopHookDTO) dto;
        if (hookDTO == null) {
            throw new IllegalArgumentException("人工介入钩子配置不能为空");
        }

        Boolean enabled = getOrDefault(hookDTO.getEnabled(), Boolean.TRUE);
        if (!enabled) {
            return null;
        }

        String defaultDesc = getOrDefault(hookDTO.getDefaultApprovalDesc(), "该操作需要人工审批确认");
        Map<String, ToolConfig> approvalOn = getOrDefault(hookDTO.getApprovalOn(), new HashMap<>());

        HumanInTheLoopHook.Builder builder = HumanInTheLoopHook.builder();
        approvalOn.forEach((toolName, toolConfig) -> {
            ToolConfig finalConfig = toolConfig != null ? toolConfig : ToolConfig.builder().description(defaultDesc).build();
            builder.approvalOn(toolName, finalConfig);
        });
        return builder.build();
    }
}
