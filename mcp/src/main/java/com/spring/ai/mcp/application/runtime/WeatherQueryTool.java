package com.spring.ai.mcp.application.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.application.manager.McpSupportManager;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.dto.WeatherMcpRuntimeConfigDTO;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.util.StringUtils;

/**
 * 文件用途：天气查询 MCP 工具
 * 核心职责：按运行时配置拼接第三方天气接口请求，并将响应安全返回给 Agent
 */
public class WeatherQueryTool {

    private final McpServerRecord record;
    private final McpServerExtDTO ext;
    private final WeatherMcpRuntimeConfigDTO runtimeConfig;
    private final McpAuthConfigDTO authConfig;
    private final McpSupportManager mcpSupportManager;
    private final HttpClient httpClient;

    public WeatherQueryTool(
            McpServerRecord record,
            McpServerExtDTO ext,
            WeatherMcpRuntimeConfigDTO runtimeConfig,
            McpAuthConfigDTO authConfig,
            McpSupportManager mcpSupportManager
    ) {
        this.record = record;
        this.ext = ext;
        this.runtimeConfig = runtimeConfig;
        this.authConfig = authConfig;
        this.mcpSupportManager = mcpSupportManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(record.getTimeoutMs() == null ? 5000L : record.getTimeoutMs()))
                .build();
    }

    /**
     * 执行天气查询。
     */
    public McpInvocationResult execute(WeatherQueryRequest request) {
        validateConfig();
        String responseText;
        try {
            HttpRequest httpRequest = buildHttpRequest(request);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("天气服务请求失败，HTTP 状态码: " + response.statusCode());
            }
            responseText = response.body();
        } catch (Exception exception) {
            throw new IllegalStateException("调用天气服务失败: " + exception.getMessage(), exception);
        }
        String responsePayloadJson = StringUtils.hasText(runtimeConfig.getResponsePath())
                ? mcpSupportManager.extractJsonPathText(responseText, runtimeConfig.getResponsePath())
                : responseText;
        return McpInvocationResult.builder()
                .toolName(buildToolName())
                .displayText(responsePayloadJson)
                .responsePayloadJson(responsePayloadJson)
                .build();
    }

    /**
     * 暴露给 Agent 的工具入口。
     */
    public String apply(WeatherQueryRequest request, ToolContext toolContext) {
        return execute(request).getDisplayText();
    }

    public String buildToolName() {
        return "mcp_weather_" + sanitize(record.getServerCode());
    }

    public String buildDescription() {
        return "调用外部天气服务获取指定地点天气。服务编码: " + record.getServerCode()
                + buildPromptHintSuffix();
    }

    private void validateConfig() {
        if (!StringUtils.hasText(runtimeConfig.getBaseUrl())) {
            throw new IllegalArgumentException("天气 MCP 缺少 baseUrl 配置");
        }
        if (!StringUtils.hasText(runtimeConfig.getLocationParamName())) {
            throw new IllegalArgumentException("天气 MCP 缺少 locationParamName 配置");
        }
    }

    private HttpRequest buildHttpRequest(WeatherQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.location)) {
            throw new IllegalArgumentException("location 不能为空");
        }
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (runtimeConfig.getStaticQueryParams() != null) {
            queryParams.putAll(runtimeConfig.getStaticQueryParams());
        }
        queryParams.put(runtimeConfig.getLocationParamName(), request.location.trim());
        if (StringUtils.hasText(runtimeConfig.getUnitsParamName())) {
            queryParams.put(runtimeConfig.getUnitsParamName(), StringUtils.hasText(request.units)
                    ? request.units.trim()
                    : runtimeConfig.getDefaultUnits());
        }
        if (StringUtils.hasText(runtimeConfig.getLanguageParamName()) && StringUtils.hasText(request.language)) {
            queryParams.put(runtimeConfig.getLanguageParamName(), request.language.trim());
        }
        if (StringUtils.hasText(authConfig.getApiKey()) && StringUtils.hasText(authConfig.getQueryParamName())) {
            queryParams.put(authConfig.getQueryParamName(), authConfig.getApiKey());
        }
        String queryText = queryParams.entrySet()
                .stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()) && entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        String normalizedBaseUrl = runtimeConfig.getBaseUrl().trim().replaceAll("/+$", "");
        String normalizedPath = StringUtils.hasText(runtimeConfig.getPath()) ? runtimeConfig.getPath().trim() : "";
        String requestUrl = normalizedBaseUrl + normalizedPath + (queryText.isEmpty() ? "" : "?" + queryText);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofMillis(record.getTimeoutMs() == null ? 5000L : record.getTimeoutMs()))
                .GET();
        if (StringUtils.hasText(authConfig.getApiKey()) && StringUtils.hasText(authConfig.getHeaderName())) {
            builder.header(authConfig.getHeaderName().trim(), authConfig.getApiKey());
        }
        if (authConfig.getExtraHeaders() != null) {
            authConfig.getExtraHeaders().forEach((key, value) -> {
                if (StringUtils.hasText(key) && value != null) {
                    builder.header(key.trim(), value);
                }
            });
        }
        return builder.build();
    }

    private String buildPromptHintSuffix() {
        return StringUtils.hasText(ext.getToolPromptHint()) ? "。补充提示: " + ext.getToolPromptHint().trim() : "";
    }

    private String sanitize(String value) {
        return value == null ? "server" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class WeatherQueryRequest {

        @JsonProperty(required = true)
        @JsonPropertyDescription("查询地点，例如 Shanghai 或 Beijing,CN")
        public String location;

        @JsonPropertyDescription("返回语言，例如 zh 或 en")
        public String language;

        @JsonPropertyDescription("温度单位，例如 metric 或 imperial")
        public String units;
    }
}
