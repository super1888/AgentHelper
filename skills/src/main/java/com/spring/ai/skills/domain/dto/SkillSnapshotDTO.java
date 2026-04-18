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

    private String skillType;

    private String skillCategory;

    private List<SkillCategoryDTO> categoryChain;

    private List<SkillTagDTO> tags;

    private String skillStatus;

    private String publishStatus;

    private String versionCode;

    private String versionDescription;

    private String versionMode;

    private Integer sortWeight;

    private Integer hotUpdateEnabled;

    private SkillObservabilityConfigDTO observabilityConfig;

    private SkillReleaseConfigDTO releaseConfig;

    private SkillBatchConfigDTO batchConfig;

    private SkillWorkflowConfigDTO workflowConfig;

    private List<SkillChannelAdaptationDTO> channelAdaptations;

    private SkillMarketplaceConfigDTO marketplaceConfig;

    private String remark;
}
