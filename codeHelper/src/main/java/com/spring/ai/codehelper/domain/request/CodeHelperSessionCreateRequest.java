package com.spring.ai.codehelper.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 代码助手会话创建请求。
 */
@Data
public class CodeHelperSessionCreateRequest {

    private String sessionName;

    private String workspacePath;

    private String projectName;

    private String branchName;

    private String taskDescription;

    private String modelCode;

    private List<String> allowedCommands;
}
