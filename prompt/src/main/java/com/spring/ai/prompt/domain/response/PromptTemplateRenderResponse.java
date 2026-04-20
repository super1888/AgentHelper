package com.spring.ai.prompt.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：封装模板试渲染结果。
 * 核心功能：返回渲染后的文本、缺失变量和命中的渲染规则摘要。
 */
@Value
@Builder
public class PromptTemplateRenderResponse {

    String renderedContent;

    List<String> missingVariables;

    List<String> appliedConditions;

    List<String> appliedLoops;
}
