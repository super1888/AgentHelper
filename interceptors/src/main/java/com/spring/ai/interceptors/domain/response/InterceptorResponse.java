package com.spring.ai.interceptors.domain.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorResponse {

    Long id;

    String interceptorCode;

    String interceptorName;

    String description;

    String interceptorType;

    String interceptorStage;

    String interceptorStatus;

    String publishStatus;

    String riskLevel;

    String triggerMode;

    String failStrategy;

    Integer sortWeight;

    Integer timeoutMs;

    Integer hotUpdateEnabled;

    Integer currentVersionNo;

    Integer latestVersionNo;

    Integer publishedVersionNo;

    String versionCode;

    String versionDescription;

    String builtinInterceptorKey;

    String scriptLanguage;

    List<String> tags;

    List<String> targetChannels;

    List<String> targetEnvironments;

    List<String> targetAgentCodes;

    List<String> targetModelCodes;

    Map<String, Object> conditionConfig;

    Map<String, Object> runtimeConfig;

    Map<String, Object> securityConfig;

    Map<String, Object> observabilityConfig;

    Map<String, Object> degradationConfig;

    Map<String, Object> interceptorConfig;

    String scriptContent;

    String testPayloadJson;

    Long tenantId;

    Long ownerUserId;

    String ownerUserName;

    Integer bindingCount;

    Integer testCaseCount;

    Integer logCount;

    String remark;

    Long createTime;

    Long updateTime;

    List<InterceptorVersionResponse> versions;
}
