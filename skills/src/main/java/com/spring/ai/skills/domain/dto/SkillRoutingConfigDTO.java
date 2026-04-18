package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillRoutingConfigDTO {

    private String routePolicy;

    private List<String> routeTags;

    private List<String> manualIntentBindings;

    private String conflictResolutionPolicy;

    private List<String> dependencySkillCodes;

    private List<String> mutexSkillCodes;

    private List<String> preCheckRules;

    private List<String> contextReadableKeys;

    private List<String> contextWritableKeys;

    private Integer contextExpireMinutes;

    private String slotFillStrategy;

    private String fallbackSkillCode;
}
