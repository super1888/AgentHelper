package com.spring.ai.prompt.domain.request;

import java.util.Map;
import lombok.Data;

/**
 * 文件用途：定义模板试渲染请求。
 * 核心功能：接收模板变量和结构化上下文，返回企业模板渲染结果。
 */
@Data
public class PromptTemplateRenderRequest {

    private Map<String, Object> variables;
}
