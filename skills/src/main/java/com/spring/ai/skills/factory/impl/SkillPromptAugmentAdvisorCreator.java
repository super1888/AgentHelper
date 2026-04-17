package com.spring.ai.skills.factory.impl;

import com.alibaba.cloud.ai.graph.advisors.SkillPromptAugmentAdvisor;
import com.spring.ai.common.enums.SkillFactoryTypeEnum;
import com.spring.ai.skills.domain.dto.SkillPromptAugmentAdvisorDTO;
import com.spring.ai.skills.factory.SkillCreator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SkillPromptAugmentAdvisor 创建器。
 */
@Component
public class SkillPromptAugmentAdvisorCreator implements SkillCreator {

    @Override
    public SkillFactoryTypeEnum getSkillFactoryType() {
        return SkillFactoryTypeEnum.SKILL_PROMPT_AUGMENT_ADVISOR;
    }

    @Override
    public Object create(Object dto) {
        SkillPromptAugmentAdvisorDTO advisorDTO = (SkillPromptAugmentAdvisorDTO) dto;
        if (advisorDTO == null) {
            throw new IllegalArgumentException("技能提示增强顾问配置不能为空");
        }

        SkillPromptAugmentAdvisor.Builder builder = new SkillPromptAugmentAdvisor.Builder();
        if (advisorDTO.getUserSkillsResource() != null) {
            builder.userSkillsDirectory(advisorDTO.getUserSkillsResource());
        }
        else if (StringUtils.hasText(advisorDTO.getUserSkillsDirectory())) {
            builder.userSkillsDirectory(advisorDTO.getUserSkillsDirectory());
        }
        if (advisorDTO.getProjectSkillsResource() != null) {
            builder.projectSkillsDirectory(advisorDTO.getProjectSkillsResource());
        }
        else if (StringUtils.hasText(advisorDTO.getProjectSkillsDirectory())) {
            builder.projectSkillsDirectory(advisorDTO.getProjectSkillsDirectory());
        }
        if (advisorDTO.getSkillRegistry() != null) {
            builder.skillRegistry(advisorDTO.getSkillRegistry());
        }
        if (advisorDTO.getOrder() != null) {
            builder.order(advisorDTO.getOrder());
        }
        if (advisorDTO.getScheduler() != null) {
            builder.scheduler(advisorDTO.getScheduler());
        }
        if (advisorDTO.getLazyLoad() != null) {
            builder.lazyLoad(advisorDTO.getLazyLoad());
        }
        return builder.build();
    }
}
