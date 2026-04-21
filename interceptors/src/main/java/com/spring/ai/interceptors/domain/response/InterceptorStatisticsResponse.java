package com.spring.ai.interceptors.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorStatisticsResponse {

    Integer totalCount;

    Integer enabledCount;

    Integer publishedCount;

    Integer hotUpdateEnabledCount;

    Integer deletedCount;

    Integer highRiskCount;

    Integer totalBindingCount;

    Integer totalTestCaseCount;

    Integer totalLogCount;

    Integer successLogCount;

    Integer failureLogCount;
}
