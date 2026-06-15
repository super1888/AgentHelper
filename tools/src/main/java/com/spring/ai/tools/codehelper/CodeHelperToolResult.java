package com.spring.ai.tools.codehelper;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具执行结果，封装执行状态、文本结果和风险等级。
 */
@Data
@Builder
public class CodeHelperToolResult {

    private String toolName;

    private String riskLevel;

    private boolean success;

    private String message;

    private String output;

    private Long durationMillis;
}
