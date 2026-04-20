package com.spring.ai.prompt.domain.response;

import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：封装提示词模板详情响应。
 * 核心功能：统一返回模板基础信息、变量定义、企业配置和时间字段。
 */
@Value
@Builder
public class PromptTemplateResponse {

    Long id;

    String templateCode;

    String templateName;

    String description;

    String templateType;

    String sourceType;

    String templateContent;

    String sourcePath;

    String templateStatus;

    Long ownerUserId;

    String ownerUserName;

    List<PromptTemplateVariableDTO> variableDefinitions;

    PromptTemplateEnterpriseConfigDTO enterpriseConfig;

    Long createTime;

    Long updateTime;
}
