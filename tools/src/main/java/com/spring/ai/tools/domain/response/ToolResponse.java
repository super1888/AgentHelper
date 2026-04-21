package com.spring.ai.tools.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：工具管理主响应对象
 */
@Data
@Builder
public class ToolResponse {

    private Long id;

    private String toolCode;

    private String toolName;

    private String description;

    private String toolType;

    private String toolCategory;

    private String sourceType;

    private String toolStatus;

    private String publishStatus;

    private String riskLevel;

    private String executionMode;

    private Integer sortWeight;

    private Integer timeoutMs;

    private Integer authRequired;

    private String builtinToolKey;

    private String endpointUrl;

    private String httpMethod;

    private List<String> tags;

    private String requestSchemaJson;

    private String authConfigJson;

    private String runtimeConfigJson;

    private String testPayloadJson;

    private Long tenantId;

    private Long ownerUserId;

    private String ownerUserName;

    private Integer logCount;

    private String remark;

    private Long createTime;

    private Long updateTime;
}
