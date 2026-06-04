package com.spring.ai.statistics.service.impl;

import com.spring.ai.statistics.domain.request.StatisticsTrackRequest;
import com.spring.ai.statistics.domain.response.StatisticsMetricPointResponse;
import com.spring.ai.statistics.domain.response.StatisticsOverviewResponse;
import com.spring.ai.statistics.domain.response.StatisticsTrackResponse;
import com.spring.ai.statistics.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final String KEY_PREFIX = "agentHelper:statistics";
    private static final String METRIC_PV = "pv";
    private static final String METRIC_VV = "vv";
    private static final String METRIC_UV = "uv";
    private static final String METRIC_IP = "ip";
    private static final int DEFAULT_DAYS = 14;
    private static final int MAX_DAYS = 90;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StringRedisTemplate stringRedisTemplate;

    public StatisticsServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public StatisticsTrackResponse track(StatisticsTrackRequest request, HttpServletRequest servletRequest) {
        StatisticsTrackRequest safeRequest = request == null ? new StatisticsTrackRequest() : request;
        String date = LocalDate.now().format(DATE_FORMATTER);
        String path = normalizePath(safeRequest.getPath());
        String visitorId = normalizeIdentifier(safeRequest.getVisitorId(), "visitor");
        String visitId = normalizeIdentifier(safeRequest.getVisitId(), "visit");
        String ipAddress = resolveClientIp(servletRequest);

        addMetric(METRIC_PV, date, UUID.randomUUID().toString());
        addMetric(METRIC_VV, date, visitId);
        addMetric(METRIC_UV, date, visitorId);
        addMetric(METRIC_IP, date, ipAddress);

        return StatisticsTrackResponse.builder()
                .date(date)
                .path(path)
                .visitorId(visitorId)
                .visitId(visitId)
                .message("访问统计已记录")
                .build();
    }

    @Override
    public StatisticsOverviewResponse overview(String startDate, String endDate) {
        LocalDate end = parseDateOrDefault(endDate, LocalDate.now());
        LocalDate start = parseDateOrDefault(startDate, end.minusDays(DEFAULT_DAYS - 1L));
        if (start.isAfter(end)) {
            LocalDate temporary = start;
            start = end;
            end = temporary;
        }
        if (start.plusDays(MAX_DAYS - 1L).isBefore(end)) {
            start = end.minusDays(MAX_DAYS - 1L);
        }

        List<StatisticsMetricPointResponse> trends = new ArrayList<>();
        long totalPv = 0L;
        long totalVv = 0L;
        long totalUv = 0L;
        long totalIp = 0L;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String date = cursor.format(DATE_FORMATTER);
            long pv = countMetric(METRIC_PV, date);
            long vv = countMetric(METRIC_VV, date);
            long uv = countMetric(METRIC_UV, date);
            long ip = countMetric(METRIC_IP, date);
            trends.add(StatisticsMetricPointResponse.builder()
                    .date(date)
                    .pv(pv)
                    .vv(vv)
                    .uv(uv)
                    .ip(ip)
                    .build());
            totalPv += pv;
            totalVv += vv;
            totalUv += uv;
            totalIp += ip;
            cursor = cursor.plusDays(1L);
        }

        String today = LocalDate.now().format(DATE_FORMATTER);
        return StatisticsOverviewResponse.builder()
                .startDate(start.format(DATE_FORMATTER))
                .endDate(end.format(DATE_FORMATTER))
                .totalPv(totalPv)
                .totalVv(totalVv)
                .totalUv(totalUv)
                .totalIp(totalIp)
                .todayPv(countMetric(METRIC_PV, today))
                .todayVv(countMetric(METRIC_VV, today))
                .todayUv(countMetric(METRIC_UV, today))
                .todayIp(countMetric(METRIC_IP, today))
                .trends(trends)
                .build();
    }

    private void addMetric(String metric, String date, String value) {
        stringRedisTemplate.opsForHyperLogLog().add(metricKey(metric, date), value);
    }

    private long countMetric(String metric, String date) {
        return stringRedisTemplate.opsForHyperLogLog().size(metricKey(metric, date));
    }

    private String metricKey(String metric, String date) {
        return KEY_PREFIX + ":" + metric + ":" + date;
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        String normalized = path.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        return StringUtils.hasText(normalized) ? normalized : "/";
    }

    private String normalizeIdentifier(String value, String prefix) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return prefix + "-" + UUID.randomUUID();
    }

    private LocalDate parseDateOrDefault(String value, LocalDate defaultDate) {
        if (!StringUtils.hasText(value)) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return defaultDate;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : "unknown";
    }
}
