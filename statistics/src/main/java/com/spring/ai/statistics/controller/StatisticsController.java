package com.spring.ai.statistics.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.statistics.domain.request.StatisticsTrackRequest;
import com.spring.ai.statistics.domain.response.StatisticsOverviewResponse;
import com.spring.ai.statistics.domain.response.StatisticsTrackResponse;
import com.spring.ai.statistics.service.StatisticsService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @PostMapping("/track")
    public ApiResponse<StatisticsTrackResponse> track(@RequestBody StatisticsTrackRequest request,
                                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(statisticsService.track(request, servletRequest));
    }

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> overview(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(statisticsService.overview(startDate, endDate));
    }
}