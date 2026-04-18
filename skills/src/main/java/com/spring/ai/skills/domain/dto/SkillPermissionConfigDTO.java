package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillPermissionConfigDTO {

    private Integer loginRequired;

    private List<String> allowedRoles;

    private List<String> allowedUserGroups;

    private List<String> allowedDepartments;

    private List<String> allowedRegions;

    private List<String> whitelistUsers;

    private List<String> blacklistUsers;

    private String approvalPolicy;
}
