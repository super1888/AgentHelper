package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillSnapshotDTO {

    private String skillCode;

    private String skillName;

    private String description;

    private String skillCategory;

    private String skillStatus;

    private String publishStatus;

    private String versionMode;

    private Integer hotUpdateEnabled;

    private List<SkillIntentConfigDTO> intentConfigs;

    private SkillExecutionConfigDTO executionConfig;

    private SkillRoutingConfigDTO routingConfig;

    private SkillPermissionConfigDTO permissionConfig;

    private SkillObservabilityConfigDTO observabilityConfig;

    private SkillReleaseConfigDTO releaseConfig;

    private SkillBatchConfigDTO batchConfig;

    private SkillWorkflowConfigDTO workflowConfig;

    private String remark;
}
