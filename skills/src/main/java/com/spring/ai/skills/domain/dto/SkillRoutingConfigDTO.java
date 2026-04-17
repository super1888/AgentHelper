package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillRoutingConfigDTO {

    private String routePolicy;

    private List<String> routeTags;

    private String contextWindowStrategy;

    private String memoryPolicy;

    private String fallbackSkillCode;
}
