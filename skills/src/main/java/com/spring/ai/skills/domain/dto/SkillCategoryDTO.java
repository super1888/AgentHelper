package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillCategoryDTO {

    private String categoryCode;

    private String categoryName;

    private String parentCode;

    private Integer categoryLevel;
}
