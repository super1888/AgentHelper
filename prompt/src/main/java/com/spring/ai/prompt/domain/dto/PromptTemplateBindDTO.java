package com.spring.ai.prompt.domain.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：描述提示词绑定关系的传输对象。
 * 核心功能：统一承载模板绑定类型、来源和模板内容信息。
 */
@Value
@Builder
public class PromptTemplateBindDTO {

    Long promptTemplateId;

    String promptBindingType;

    String promptSourceType;

    String promptTemplateContent;

    String promptTemplatePath;
}
