package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillPermissionConfigDTO {

    private List<String> allowedRoles;

    private List<String> dataScopes;

    private String approvalPolicy;

    private String riskLevel;

    private String riskControlPolicy;
}
