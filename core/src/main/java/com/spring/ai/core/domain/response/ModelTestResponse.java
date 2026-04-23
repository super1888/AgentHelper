package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelTestResponse {

    Boolean success;

    String providerEnum;

    String modelIdentifier;

    String responseContent;

    Long elapsedMs;
}
