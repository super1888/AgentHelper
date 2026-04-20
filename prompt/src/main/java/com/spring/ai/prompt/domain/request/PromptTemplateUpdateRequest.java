package com.spring.ai.prompt.domain.request;

import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Data;

/**
 * 文件用途：定义更新提示词模板时的请求参数。
 * 核心功能：承载模板元信息、来源配置、启停状态和扩展规则。
 */
@Data
public class PromptTemplateUpdateRequest {

    private String templateName;

    private String description;

    private String sourceType;

    private String templateContent;

    private String sourcePath;

    private String templateStatus;

    private List<PromptTemplateVariableDTO> variableDefinitions;

    private PromptTemplateEnterpriseConfigDTO enterpriseConfig;
}
