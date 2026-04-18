package com.spring.ai.skills.domain.response;

import com.spring.ai.skills.domain.dto.SkillBatchConfigDTO;
import com.spring.ai.skills.domain.dto.SkillCategoryDTO;
import com.spring.ai.skills.domain.dto.SkillChannelAdaptationDTO;
import com.spring.ai.skills.domain.dto.SkillMarketplaceConfigDTO;
import com.spring.ai.skills.domain.dto.SkillObservabilityConfigDTO;
import com.spring.ai.skills.domain.dto.SkillReleaseConfigDTO;
import com.spring.ai.skills.domain.dto.SkillTagDTO;
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

    private String skillType;

    private String skillCategory;

    private List<SkillCategoryDTO> categoryChain;

    private List<SkillTagDTO> tags;

    private String skillStatus;

    private String publishStatus;

    private Integer sortWeight;

    private String versionCode;

    private String versionDescription;

    private String versionMode;

    private Integer currentVersionNo;

    private Integer latestVersionNo;

    private Integer publishedVersionNo;

    private Integer hotUpdateEnabled;

    private Long tenantId;

    private Long ownerUserId;

    private String ownerUserName;

    private SkillObservabilityConfigDTO observabilityConfig;

    private SkillReleaseConfigDTO releaseConfig;

    private SkillBatchConfigDTO batchConfig;

    private SkillWorkflowConfigDTO workflowConfig;

    private List<SkillChannelAdaptationDTO> channelAdaptations;

    private SkillMarketplaceConfigDTO marketplaceConfig;

    private List<SkillVersionResponse> versions;

    private Integer testCaseCount;

    private Integer logCount;

    private String remark;

    private Long createTime;

    private Long updateTime;
}
