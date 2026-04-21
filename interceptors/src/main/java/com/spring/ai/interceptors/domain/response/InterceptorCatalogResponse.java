package com.spring.ai.interceptors.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorCatalogResponse {

    String interceptorKey;

    String interceptorName;

    String description;

    String interceptorType;

    String interceptorStage;

    String riskLevel;

    String failStrategy;

    String defaultConfigJson;

    String defaultTestPayloadJson;

    List<String> tags;
}
