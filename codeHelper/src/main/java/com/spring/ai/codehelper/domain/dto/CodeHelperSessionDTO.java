package com.spring.ai.codehelper.domain.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手会话 DTO。
 */
@Data
@Builder
public class CodeHelperSessionDTO {

    private String sessionId;

    private String sessionName;

    private String workspacePath;

    private String projectName;

    private String branchName;

    private String taskDescription;

    private String modelCode;

    private String status;

    private String summary;

    @Builder.Default
    private List<CodeHelperMessageDTO> messages = new ArrayList<>();

    @Builder.Default
    private List<CodeHelperTaskDTO> tasks = new ArrayList<>();

    @Builder.Default
    private List<String> allowedCommands = new ArrayList<>();
}
