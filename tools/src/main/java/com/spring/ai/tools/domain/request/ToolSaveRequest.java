package com.spring.ai.tools.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 文件用途：工具新增与编辑请求对象
 */
@Data
public class ToolSaveRequest {

    private String toolCode;

    private String toolName;

    private String description;

    private String toolType;

    private String toolCategory;

    private String sourceType;

    private String toolStatus;

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

    private String remark;
}
