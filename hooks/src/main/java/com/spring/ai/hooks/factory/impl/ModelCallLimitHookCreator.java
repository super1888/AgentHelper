package com.spring.ai.hooks.factory.impl;

import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.ModelCallLimitHookDTO;
import com.spring.ai.hooks.factory.AbstractHookCreator;
import org.springframework.stereotype.Component;

/**
 * 模型调用次数限制 Hook 创建器。
 */
@Component
public class ModelCallLimitHookCreator extends AbstractHookCreator {

    @Override
    public HookTypeEnum getHookType() {
        return HookTypeEnum.MODEL_CALL_LIMIT;
    }

    @Override
    public Object create(Object dto) {
        ModelCallLimitHookDTO hookDTO = (ModelCallLimitHookDTO) dto;
        if (hookDTO == null) {
            throw new IllegalArgumentException("ModelCallLimitHookDTO 不能为空");
        }

        ModelCallLimitHook.Builder builder = ModelCallLimitHook.builder();
        if (hookDTO.getRunLimit() != null) {
            builder.runLimit(hookDTO.getRunLimit());
        }
        if (hookDTO.getThreadLimit() != null) {
            builder.threadLimit(hookDTO.getThreadLimit());
        }
        if (hookDTO.getExitBehavior() != null) {
            builder.exitBehavior(hookDTO.getExitBehavior());
        }
        return builder.build();
    }
}
