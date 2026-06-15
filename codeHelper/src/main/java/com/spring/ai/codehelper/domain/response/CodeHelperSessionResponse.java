package com.spring.ai.codehelper.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手会话响应。
 */
@Data
@Builder
public class CodeHelperSessionResponse {

    private String sessionId;

    private String sessionName;

    private String workspacePath;

    private String projectName;

    private String branchName;

    private String taskDescription;

    private String modelCode;

    private String status;

    private String summary;

    private List<CodeHelperMessageResponse> messages;

    private List<CodeHelperTaskResponse> tasks;
}
