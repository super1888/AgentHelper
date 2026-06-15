package com.spring.ai.tools.codehelper;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具描述，用于向 Agent Runtime 和前端暴露可用工具清单。
 */
@Data
@Builder
public class CodeHelperToolDescriptor {

    private String toolName;

    private String displayName;

    private String description;

    private String riskLevel;

    private List<String> argumentNames;
}
