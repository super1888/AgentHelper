package com.spring.ai.logging.web;

import com.spring.ai.logging.config.AgentHelperLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 为请求注入 traceId，并统一输出访问日志。
 */
public class RequestTraceLogFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("com.spring.ai.logging.access");

    private static final String TRACE_ID_KEY = "traceId";

    private final AgentHelperLoggingProperties loggingProperties;

    public RequestTraceLogFilter(AgentHelperLoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        String headerName = loggingProperties.getTrace().getHeaderName();
        String traceId = request.getHeader(headerName);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(TRACE_ID_KEY, traceId);
        if (loggingProperties.getTrace().isResponseHeaderEnabled()) {
            response.setHeader(headerName, traceId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!shouldSkipAccessLog(request)) {
                writeAccessLog(request, response, traceId, startNs);
            }
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private boolean shouldSkipAccessLog(HttpServletRequest request) {
        if (!loggingProperties.getAccess().isEnabled()) {
            return true;
        }
        String requestUri = request.getRequestURI();
        List<String> excludePathPrefixes = loggingProperties.getAccess().getExcludePathPrefixes();
        if (CollectionUtils.isEmpty(excludePathPrefixes)) {
            return false;
        }
        return excludePathPrefixes.stream()
                .filter(StringUtils::hasText)
                .anyMatch(requestUri::startsWith);
    }

    private void writeAccessLog(HttpServletRequest request, HttpServletResponse response, String traceId, long startNs) {
        long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        StringBuilder builder = new StringBuilder(256);
        builder.append("traceId=").append(traceId)
                .append(", method=").append(request.getMethod())
                .append(", uri=").append(buildUri(request))
                .append(", status=").append(response.getStatus())
                .append(", costMs=").append(costMs)
                .append(", clientIp=").append(resolveClientIp(request));
        if (loggingProperties.getAccess().isLogRequestParameters()) {
            builder.append(", params=").append(extractParameters(request));
        }
        if (loggingProperties.getAccess().isLogRequestHeaders()) {
            builder.append(", headers=").append(extractHeaders(request));
        }
        ACCESS_LOGGER.info(builder.toString());
    }

    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (!StringUtils.hasText(queryString)) {
            return uri;
        }
        return uri + "?" + truncate(URLDecoder.decode(queryString, StandardCharsets.UTF_8));
    }

    private String extractParameters(HttpServletRequest request) {
        if (isMultipartRequest(request)) {
            return "[multipart omitted]";
        }
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "[]";
        }
        List<String> items = new ArrayList<>();
        parameterMap.forEach((key, values) -> items.add(key + "=" + truncate(String.join("|", values))));
        return items.toString();
    }

    private String extractHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return "[]";
        }
        return Collections.list(headerNames).stream()
                .map(name -> name + "=" + truncate(maskHeaderValue(name, request.getHeader(name))))
                .collect(Collectors.toList())
                .toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isMultipartRequest(HttpServletRequest request) {
        return StringUtils.hasText(request.getContentType())
                && request.getContentType().toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private String maskHeaderValue(String headerName, String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String lowerCaseName = headerName.toLowerCase();
        if (lowerCaseName.contains("authorization") || lowerCaseName.contains("token")
                || lowerCaseName.contains("secret") || lowerCaseName.contains("cookie")) {
            return "***";
        }
        return value;
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int maxLength = Math.max(100, loggingProperties.getAccess().getMaxBodyLength());
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }
}
