package com.spring.ai.skills.domain.dto;

import java.util.Map;
import lombok.Data;

@Data
public class SkillExecutionConfigDTO {

    private String actionType;

    private Integer timeoutMs;

    private Integer retryTimes;

    private Map<String, Object> httpConfig;

    private Map<String, Object> grpcConfig;

    private Map<String, Object> databaseConfig;

    private Map<String, Object> pluginConfig;

    private Map<String, Object> functionConfig;

    private Map<String, Object> scriptConfig;

    private Map<String, Object> workflowJumpConfig;
}
