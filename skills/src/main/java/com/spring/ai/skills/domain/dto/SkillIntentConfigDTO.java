package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillIntentConfigDTO {

    private String intentCode;

    private String intentName;

    private String boundIntent;

    private List<String> keywords;

    private List<String> regexPatterns;

    private List<String> similarPhrases;

    private Double confidenceThreshold;

    private Integer exactMatchEnabled;

    private Integer fuzzyMatchEnabled;

    private List<String> contextDependencyKeys;

    private List<String> previousSkillCodes;

    private List<SkillParameterConfigDTO> parameterConfigs;
}
