package com.spring.ai.interceptors.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：Interceptor 版本快照对象
 * 核心职责：封装页面配置与版本归档需要的完整快照内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterceptorSnapshotDTO {

    private String interceptorCode;

    private String interceptorName;

    private String description;

    private String interceptorType;

    private String interceptorStage;

    private String interceptorStatus;

    private String publishStatus;

    private String riskLevel;

    private String triggerMode;

    private String failStrategy;

    private Integer sortWeight;

    private Integer timeoutMs;

    private Integer hotUpdateEnabled;

    private String versionCode;

    private String versionDescription;

    private String builtinInterceptorKey;

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

    private Map<String, Object> interceptorConfig;

    private String scriptContent;

    private String testPayloadJson;
}
