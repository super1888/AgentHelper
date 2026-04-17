package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillReleaseConfigDTO {

    private Integer hotUpdateEnabled;

    private String releaseChannel;

    private String grayPolicy;

    private String rollbackPolicy;
}
