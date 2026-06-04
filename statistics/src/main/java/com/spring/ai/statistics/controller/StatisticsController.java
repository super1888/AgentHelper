package com.spring.ai.statistics.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.statistics.domain.request.StatisticsTrackRequest;
import com.spring.ai.statistics.domain.response.StatisticsOverviewResponse;
import com.spring.ai.statistics.domain.response.StatisticsTrackResponse;
import com.spring.ai.statistics.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问统计接口，提供访问打点和统计概览查询能力。
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 记录一次访问行为，用于 PV、VV、UV、IP 指标统计。
     */
    @PostMapping("/track")
    public ApiResponse<StatisticsTrackResponse> track(@RequestBody StatisticsTrackRequest request,
                                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(statisticsService.track(request, servletRequest));
    }

    /**
     * 查询指定日期范围内的访问统计概览和趋势。
     */
    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> overview(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(statisticsService.overview(startDate, endDate));
    }
}