package com.spring.ai.core.domain.request;

import lombok.Data;

@Data
public class ModelDefinitionSaveRequest {

    private String modelName;

    private String providerConfigCode;

    private String modelType;

    private String modelIdentifier;

    private Double temperature;

    private Double topP;

    private Double presencePenalty;

    private Double frequencyPenalty;

    private Integer maxTokens;

    private Integer contextWindow;

    private Integer rpmLimit;

    private Integer tpmLimit;

    private Integer timeoutMs;

    private Boolean supportStreaming;

    private Boolean supportTools;

    private Boolean supportVision;

    private Boolean supportJsonSchema;

    private Boolean defaultModel;

    private String advancedConfigJson;

    private String remark;

    private String status;
}
