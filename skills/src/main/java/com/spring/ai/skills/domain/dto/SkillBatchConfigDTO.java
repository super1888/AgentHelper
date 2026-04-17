package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillBatchConfigDTO {

    private Integer batchEnabled;

    private Integer importEnabled;

    private Integer exportEnabled;

    private String importTemplate;

    private String exportTemplate;
}
