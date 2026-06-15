package com.spring.ai.codehelper.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 代码助手权限校验请求。
 */
@Data
public class CodeHelperPermissionCheckRequest {

    private String toolName;

    private String workspacePath;

    private String command;

    private List<String> allowedCommands;
}
