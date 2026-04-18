package com.spring.ai.skills.domain.request;

import lombok.Data;

@Data
public class SkillVersionCompareRequest {

    private Integer sourceVersionNo;

    private Integer targetVersionNo;
}
