package com.spring.ai.skills.domain.request;

import com.spring.ai.skills.domain.dto.SkillBatchConfigDTO;
import com.spring.ai.skills.domain.dto.SkillExecutionConfigDTO;
import com.spring.ai.skills.domain.dto.SkillIntentConfigDTO;
import com.spring.ai.skills.domain.dto.SkillObservabilityConfigDTO;
import com.spring.ai.skills.domain.dto.SkillPermissionConfigDTO;
import com.spring.ai.skills.domain.dto.SkillReleaseConfigDTO;
import com.spring.ai.skills.domain.dto.SkillRoutingConfigDTO;
import com.spring.ai.skills.domain.dto.SkillWorkflowConfigDTO;
import java.util.List;
import lombok.Data;

@Data
public class SkillSaveRequest {

    private String skillCode;

    private String skillName;

    private String description;

    private String skillCategory;

    private String skillStatus;

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
