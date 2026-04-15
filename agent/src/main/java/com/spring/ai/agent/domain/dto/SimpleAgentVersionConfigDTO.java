package com.spring.ai.agent.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 版本快照配置。
 *
 * <p>每次保存 Agent 时都会固化一份快照，
 * 会话只绑定某个确定版本，保证历史行为可追溯。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAgentVersionConfigDTO {

    /**
     * Agent 名称快照。
     */
    private String agentName;

    /**
     * 描述快照。
     */
    private String description;

    /**
     * 系统提示词快照。
     */
    private String systemPrompt;

    /**
     * 能力选择快照。
     */
    private List<String> selectedCapabilities;
}
