package com.spring.ai.hooks.domain.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 详情响应
 */
@Data
@Builder
public class HookResponse {

    private Long id;
    private String hookCode;
    private String hookName;
    private String description;
    private String hookType;
    private String hookStage;
    private String hookStatus;
    private String publishStatus;
    private String riskLevel;
    private String triggerMode;
    private String failStrategy;
    private Integer sortWeight;
    private Integer timeoutMs;
    private Integer hotUpdateEnabled;
    private Integer currentVersionNo;
    private Integer latestVersionNo;
    private Integer publishedVersionNo;
    private String versionCode;
    private String versionDescription;
    private String builtinHookKey;
    private String scriptLanguage;
    private List<String> tags;
    private List<String> targetChannels;
    private List<String> targetEnvironments;
    private List<String> targetAgentCodes;
    private List<String> targetModelCodes;
    private Map<String, Object> conditionConfig;
    private Map<String, Object> runtimeConfig;
    private Map<String, Object> securityConfig;
    private Map<String, Object> observabilityConfig;
    private Map<String, Object> degradationConfig;
    private String scriptContent;
    private String testPayloadJson;
    private Long tenantId;
    private Long ownerUserId;
    private String ownerUserName;
    private Integer bindingCount;
    private Integer testCaseCount;
    private Integer logCount;
    private String remark;
    private Long createTime;
    private Long updateTime;
    private List<HookVersionResponse> versions;
}
