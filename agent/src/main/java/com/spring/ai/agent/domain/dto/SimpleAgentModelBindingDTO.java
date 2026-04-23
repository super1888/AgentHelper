package com.spring.ai.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAgentModelBindingDTO {

    private String modelCode;

    private String modelName;

    private String providerConfigCode;

    private String providerEnum;

    private String providerName;

    private String modelIdentifier;

    private String modelType;
}
