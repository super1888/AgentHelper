package com.spring.ai.skills.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillVersionCompareResponse {

    private Integer sourceVersionNo;

    private Integer targetVersionNo;

    private String sourceSnapshotJson;

    private String targetSnapshotJson;

    private String diffSummary;
}
