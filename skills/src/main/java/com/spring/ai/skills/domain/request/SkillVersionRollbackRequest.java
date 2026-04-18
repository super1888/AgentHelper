package com.spring.ai.skills.domain.request;

import lombok.Data;

@Data
public class SkillVersionRollbackRequest {

    private Integer targetVersionNo;

    private String versionDescription;
}
