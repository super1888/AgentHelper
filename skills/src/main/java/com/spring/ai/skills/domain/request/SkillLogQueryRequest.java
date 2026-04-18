package com.spring.ai.skills.domain.request;

import lombok.Data;

@Data
public class SkillLogQueryRequest {

    private Long skillId;

    private String sourceType;

    private Integer successFlag;
}
