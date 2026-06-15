package com.spring.ai.codehelper.domain.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 代码助手工具执行请求。
 */
@Data
public class CodeHelperToolExecuteRequest {

    private String sessionId;

    private String toolName;

    private String workspacePath;

    private Map<String, Object> arguments;

    private List<String> allowedCommands;
}
