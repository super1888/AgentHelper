package com.spring.ai.statistics.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsMetricPointResponse {

    private String date;
    private Long pv;
    private Long vv;
    private Long uv;
    private Long ip;
}