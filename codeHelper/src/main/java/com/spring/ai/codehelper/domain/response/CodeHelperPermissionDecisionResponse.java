package com.spring.ai.codehelper.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手权限校验响应。
 */
@Data
@Builder
public class CodeHelperPermissionDecisionResponse {

    private String toolName;

    private boolean allowed;

    private String reason;

    private String riskLevel;
}
