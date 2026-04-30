package com.spring.ai.logging.web;

import com.spring.ai.logging.config.AgentHelperLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每次请求注入 traceId，便于线上问题定位。
 */
public class RequestTraceLogFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    private final AgentHelperLoggingProperties loggingProperties;

    public RequestTraceLogFilter(AgentHelperLoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
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
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
