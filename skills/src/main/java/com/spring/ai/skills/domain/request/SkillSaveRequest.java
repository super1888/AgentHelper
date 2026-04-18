package com.spring.ai.skills.domain.request;

import com.spring.ai.skills.domain.dto.SkillBatchConfigDTO;
import com.spring.ai.skills.domain.dto.SkillCategoryDTO;
import com.spring.ai.skills.domain.dto.SkillChannelAdaptationDTO;
import com.spring.ai.skills.domain.dto.SkillMarketplaceConfigDTO;
import com.spring.ai.skills.domain.dto.SkillObservabilityConfigDTO;
import com.spring.ai.skills.domain.dto.SkillReleaseConfigDTO;
import com.spring.ai.skills.domain.dto.SkillTagDTO;
import com.spring.ai.skills.domain.dto.SkillWorkflowConfigDTO;
import java.util.List;
import lombok.Data;

@Data
public class SkillSaveRequest {

    private String skillCode;

    private String skillName;

    private String description;

    private String skillType;

    private String skillCategory;

    private List<SkillCategoryDTO> categoryChain;

    private List<SkillTagDTO> tags;

    private String skillStatus;

    private Integer sortWeight;

    private String versionCode;

    private String versionDescription;

    private String versionMode;

    private Integer hotUpdateEnabled;

    private SkillObservabilityConfigDTO observabilityConfig;

    private SkillReleaseConfigDTO releaseConfig;

    private SkillBatchConfigDTO batchConfig;

    private SkillWorkflowConfigDTO workflowConfig;

    private List<SkillChannelAdaptationDTO> channelAdaptations;

    private SkillMarketplaceConfigDTO marketplaceConfig;

    private String remark;
}
