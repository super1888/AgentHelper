package com.spring.ai.statistics.service;

import com.spring.ai.statistics.domain.request.StatisticsTrackRequest;
import com.spring.ai.statistics.domain.response.StatisticsOverviewResponse;
import com.spring.ai.statistics.domain.response.StatisticsTrackResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface StatisticsService {

    StatisticsTrackResponse track(StatisticsTrackRequest request, HttpServletRequest servletRequest);

    StatisticsOverviewResponse overview(String startDate, String endDate);
}