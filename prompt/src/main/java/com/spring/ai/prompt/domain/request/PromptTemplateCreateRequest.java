package com.spring.ai.prompt.domain.request;

import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Data;

/**
 * 文件用途：定义创建提示词模板时的请求参数。
 * 核心功能：承载模板编码、来源、变量定义和企业配置。
 */
@Data
public class PromptTemplateCreateRequest {

    private String templateCode;

    private String templateName;

    private String description;

    private String sourceType;

    private String templateContent;

    private String sourcePath;

    private List<PromptTemplateVariableDTO> variableDefinitions;

    private PromptTemplateEnterpriseConfigDTO enterpriseConfig;
}
