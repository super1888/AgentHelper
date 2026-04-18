package com.spring.ai.skills.domain.request;

import java.util.List;
import lombok.Data;

@Data
public class SkillBatchActionRequest {

    private List<Long> skillIds;

    private String skillStatus;

    private String targetCategoryCode;

    private List<String> tagNames;

    private String versionDescription;
}
