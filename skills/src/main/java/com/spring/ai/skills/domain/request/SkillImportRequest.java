package com.spring.ai.skills.domain.request;

import lombok.Data;

@Data
public class SkillImportRequest {

    private String importPayload;

    private String importFormat;

    private Integer publishAfterImport;
}
