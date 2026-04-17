package com.spring.ai.skills.domain.response;

import com.spring.ai.skills.domain.dto.SkillBatchConfigDTO;
import com.spring.ai.skills.domain.dto.SkillExecutionConfigDTO;
import com.spring.ai.skills.domain.dto.SkillIntentConfigDTO;
import com.spring.ai.skills.domain.dto.SkillObservabilityConfigDTO;
import com.spring.ai.skills.domain.dto.SkillPermissionConfigDTO;
import com.spring.ai.skills.domain.dto.SkillReleaseConfigDTO;
import com.spring.ai.skills.domain.dto.SkillRoutingConfigDTO;
import com.spring.ai.skills.domain.dto.SkillWorkflowConfigDTO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillResponse {

    private Long id;

    private String skillCode;

    private String skillName;

    private String description;

    private String skillCategory;

    private String skillStatus;

    private String publishStatus;

    private String versionMode;

    private Integer currentVersionNo;

    private Integer latestVersionNo;

    private Integer publishedVersionNo;

    private Integer hotUpdateEnabled;

    private Long tenantId;

    private Long ownerUserId;

    private String ownerUserName;

    private List<SkillIntentConfigDTO> intentConfigs;

    private SkillExecutionConfigDTO executionConfig;

    private SkillRoutingConfigDTO routingConfig;

    private SkillPermissionConfigDTO permissionConfig;

    private SkillObservabilityConfigDTO observabilityConfig;

    private SkillReleaseConfigDTO releaseConfig;

    private SkillBatchConfigDTO batchConfig;

    private SkillWorkflowConfigDTO workflowConfig;

    private List<SkillVersionResponse> versions;

    private String remark;

    private Long createTime;

    private Long updateTime;
}
