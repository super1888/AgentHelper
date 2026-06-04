package com.spring.ai.gateway.filter;

import com.spring.ai.gateway.config.GatewayAccessProperties;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关统一鉴权过滤器，后续逐步承接 quickStart 内的登录态校验逻辑。
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_HEADER = "Authorization";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private GatewayAccessProperties accessProperties;

    /**
     * 执行网关鉴权并将通过校验的请求继续转发到下游服务。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!accessProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path) || hasToken(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        return writeUnauthorized(exchange.getResponse());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        return accessProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean hasToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(TOKEN_HEADER);
        return StringUtils.hasText(token);
    }

    private Mono<Void> writeUnauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] body = "{\"success\":false,\"code\":\"401\",\"message\":\"请先登录后再访问\"}".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}