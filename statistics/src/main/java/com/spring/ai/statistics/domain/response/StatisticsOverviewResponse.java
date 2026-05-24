package com.spring.ai.statistics.domain.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StatisticsOverviewResponse {

    private String startDate;
    private String endDate;
    private Long totalPv;
    private Long totalVv;
    private Long totalUv;
    private Long totalIp;
    private Long todayPv;
    private Long todayVv;
    private Long todayUv;
    private Long todayIp;
    private List<StatisticsMetricPointResponse> trends;
}