package com.spring.ai.skills.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillVersionResponse {

    private Long id;

    private Integer versionNo;

    private String versionStatus;

    private String publishStatus;

    private Long createTime;
}
