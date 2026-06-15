package com.spring.ai.codehelper.domain.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手决策 DTO，表示模型或规则引擎给出的下一步动作。
 */
@Data
@Builder
public class CodeHelperAgentDecisionDTO {

    private String assistantReply;

    @Builder.Default
    private List<CodeHelperToolCallDTO> toolCalls = new ArrayList<>();

    private boolean requireConfirmation;

    private boolean modelDriven;
}
