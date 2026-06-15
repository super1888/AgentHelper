package com.spring.ai.tools.codehelper;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具执行请求，统一承载工具名称、工作区和参数。
 */
@Data
@Builder
public class CodeHelperToolRequest {

    private String toolName;

    private String workspacePath;

    private Map<String, Object> arguments;

    private List<String> allowedCommands;
}
