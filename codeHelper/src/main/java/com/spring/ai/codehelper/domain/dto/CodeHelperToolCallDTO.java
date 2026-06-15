package com.spring.ai.codehelper.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具调用 DTO。
 */
@Data
@Builder
public class CodeHelperToolCallDTO {

    private String toolName;

    private String workspacePath;

    private Map<String, Object> arguments;

    private List<String> allowedCommands;
}
