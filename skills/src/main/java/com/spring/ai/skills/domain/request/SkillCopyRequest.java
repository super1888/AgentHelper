package com.spring.ai.skills.domain.request;

import lombok.Data;

@Data
public class SkillCopyRequest {

    private String newSkillCode;

    private String newSkillName;

    private Integer includeTestCases;
}
