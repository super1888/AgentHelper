package com.spring.ai.agent.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAgentPromptConfigDTO {

    /**
     * 绑定的提示词模板主键，统一使用字符串承接前端入参和版本快照，避免超长整型精度丢失。
     */
    private String promptTemplateId;

    private String promptTemplateCode;

    private String promptTemplateName;

    private String promptBindingType;

    private String promptSourceType;

    private String promptTemplatePath;

    private String promptTemplateContent;

    private List<AgentPromptTemplateVariableDTO> promptVariableDefinitions;

    private Map<String, String> promptVariables;

    private PromptTemplateEnterpriseConfigDTO enterpriseConfig;
}
