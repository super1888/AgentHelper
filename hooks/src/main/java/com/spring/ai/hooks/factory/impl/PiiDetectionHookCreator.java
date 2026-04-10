package com.spring.ai.hooks.factory.impl;

import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.PIIDetectionHookDTO;
import com.spring.ai.hooks.factory.AbstractHookCreator;
import org.springframework.stereotype.Component;

/**
 * PII 检测 Hook 创建器。
 */
@Component
public class PiiDetectionHookCreator extends AbstractHookCreator {

    @Override
    public HookTypeEnum getHookType() {
        return HookTypeEnum.PII_DETECTION;
    }

    @Override
    public Object create(Object dto) {
        PIIDetectionHookDTO hookDTO = (PIIDetectionHookDTO) dto;
        if (hookDTO == null) {
            throw new IllegalArgumentException("PIIDetectionHookDTO 不能为空");
        }

        PIIDetectionHook.Builder piiBuilder = PIIDetectionHook.builder();
        if (hookDTO.getPiiType() != null) {
            piiBuilder.piiType(hookDTO.getPiiType());
        }
        if (hookDTO.getStrategy() != null) {
            piiBuilder.strategy(hookDTO.getStrategy());
        }
        if (hookDTO.getDetector() != null) {
            piiBuilder.detector(hookDTO.getDetector());
        }
        piiBuilder.applyToInput(hookDTO.isApplyToInput())
                .applyToOutput(hookDTO.isApplyToOutput())
                .applyToToolResults(hookDTO.isApplyToToolResults());
        return piiBuilder.build();
    }
}
