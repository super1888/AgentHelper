package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillIntentConfigDTO {

    private String intentName;

    private List<String> keywords;

    private List<SkillParameterConfigDTO> parameterConfigs;
}
