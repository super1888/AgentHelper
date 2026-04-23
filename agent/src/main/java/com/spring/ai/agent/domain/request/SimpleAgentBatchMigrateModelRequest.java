package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

@Data
public class SimpleAgentBatchMigrateModelRequest {

    private List<String> agentIds;

    private String targetModelConfigCode;

    /**
     * 迁移模式：DRAFT_ONLY / PUBLISH_NEW_VERSION
     */
    private String migrationMode;
}
