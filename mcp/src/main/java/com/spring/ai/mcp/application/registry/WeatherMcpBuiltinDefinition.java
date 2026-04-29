package com.spring.ai.mcp.application.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.application.assembler.McpAssembler;
import com.spring.ai.mcp.application.manager.McpInvocationLogManager;
import com.spring.ai.mcp.application.manager.McpSupportManager;
import com.spring.ai.mcp.application.model.McpBuiltinServerDefinition;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.application.runtime.WeatherQueryTool;
import com.spring.ai.mcp.config.McpManagementConstants;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.dto.WeatherMcpRuntimeConfigDTO;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：天气内置 MCP 服务定义
 */
@Component
public class WeatherMcpBuiltinDefinition implements McpBuiltinServerDefinition {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private McpSupportManager mcpSupportManager;

    @Resource
    private McpInvocationLogManager mcpInvocationLogManager;

    @Override
    public String getBuiltinServerKey() {
        return McpManagementConstants.BUILTIN_SERVER_WEATHER;
    }

    @Override
    public McpCatalogResponse buildCatalog() {
        return McpAssembler.toCatalogResponse(
                getBuiltinServerKey(),
                "天气查询 MCP",
                "通过第三方天气 API 查询指定地点天气，适合客服、出行和调度 Agent。",
                McpManagementConstants.SERVER_TYPE_BUILTIN,
                McpManagementConstants.TRANSPORT_TYPE_HTTP,
                McpManagementConstants.RISK_LEVEL_LOW,
                1,
                List.of("mcp_weather_<serverCode>"),
                toJson(WeatherMcpRuntimeConfigDTO.builder()
                        .baseUrl("https://api.openweathermap.org")
                        .path("/data/2.5/weather")
                        .locationParamName("q")
                        .unitsParamName("units")
                        .languageParamName("lang")
                        .defaultUnits("metric")
                        .responsePath(null)
                        .build()),
                """
                        {"authType":"API_KEY","queryParamName":"appid","apiKey":"replace-with-your-key"}
                        """,
                """
                        {"location":"Shanghai","language":"zh","units":"metric"}
                        """,
                "优先返回实时天气、体感温度、风速和天气描述。"
        );
    }

    @Override
    public List<ToolCallback> createToolCallbacks(McpServerRecord record, McpServerExtDTO ext) {
        WeatherQueryTool weatherQueryTool = new WeatherQueryTool(
                record,
                ext,
                parseRuntimeConfig(ext),
                parseAuthConfig(ext),
                mcpSupportManager
        );
        ToolCallback callback = FunctionToolCallback.builder(weatherQueryTool.buildToolName(), (WeatherQueryTool.WeatherQueryRequest request, ToolContext toolContext) ->
                        invokeWithRuntimeLog(
                                record,
                                weatherQueryTool.buildToolName(),
                                request,
                                () -> weatherQueryTool.execute(request).getDisplayText()))
                .description(weatherQueryTool.buildDescription())
                .inputType(WeatherQueryTool.WeatherQueryRequest.class)
                .build();
        return List.of(callback);
    }

    @Override
    public McpInvocationResult debugInvoke(McpServerRecord record, McpServerExtDTO ext, McpAuthConfigDTO authConfig, String requestPayloadJson) {
        try {
            WeatherQueryTool weatherQueryTool = new WeatherQueryTool(
                    record,
                    ext,
                    parseRuntimeConfig(ext),
                    authConfig == null ? parseAuthConfig(ext) : authConfig,
                    mcpSupportManager
            );
            WeatherQueryTool.WeatherQueryRequest request = objectMapper.readValue(
                    StringUtils.hasText(requestPayloadJson) ? requestPayloadJson.trim() : "{}",
                    WeatherQueryTool.WeatherQueryRequest.class);
            return weatherQueryTool.execute(request);
        } catch (Exception exception) {
            throw new IllegalStateException("天气 MCP 调试失败: " + exception.getMessage(), exception);
        }
    }

    private WeatherMcpRuntimeConfigDTO parseRuntimeConfig(McpServerExtDTO ext) {
        try {
            if (!StringUtils.hasText(ext.getRuntimeConfigJson())) {
                return new WeatherMcpRuntimeConfigDTO();
            }
            return objectMapper.readValue(ext.getRuntimeConfigJson(), WeatherMcpRuntimeConfigDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("天气 MCP 运行时配置解析失败: " + exception.getMessage(), exception);
        }
    }

    private McpAuthConfigDTO parseAuthConfig(McpServerExtDTO ext) {
        try {
            if (!StringUtils.hasText(ext.getAuthConfigJson())) {
                return new McpAuthConfigDTO();
            }
            return objectMapper.readValue(ext.getAuthConfigJson(), McpAuthConfigDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("天气 MCP 鉴权配置解析失败: " + exception.getMessage(), exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private <T> String invokeWithRuntimeLog(
            McpServerRecord record,
            String toolName,
            T request,
            SupplierWithException<String> supplier
    ) {
        Instant start = Instant.now();
        String requestJson = toJson(request);
        try {
            String response = supplier.get();
            mcpInvocationLogManager.saveLog(
                    record,
                    toolName,
                    requestJson,
                    response,
                    null,
                    Duration.between(start, Instant.now()).toMillis(),
                    McpManagementConstants.LOG_SOURCE_RUNTIME,
                    1
            );
            return response;
        } catch (Exception exception) {
            mcpInvocationLogManager.saveLog(
                    record,
                    toolName,
                    requestJson,
                    null,
                    exception.getMessage(),
                    Duration.between(start, Instant.now()).toMillis(),
                    McpManagementConstants.LOG_SOURCE_RUNTIME,
                    0
            );
            throw exception;
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {

        T get();
    }
}
