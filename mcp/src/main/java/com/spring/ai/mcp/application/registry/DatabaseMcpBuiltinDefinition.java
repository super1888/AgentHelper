package com.spring.ai.mcp.application.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.application.assembler.McpAssembler;
import com.spring.ai.mcp.application.manager.McpInvocationLogManager;
import com.spring.ai.mcp.application.model.McpBuiltinServerDefinition;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.application.runtime.DatabaseQueryTool;
import com.spring.ai.mcp.application.runtime.DatabaseSchemaTool;
import com.spring.ai.mcp.config.McpManagementConstants;
import com.spring.ai.mcp.domain.dto.DatabaseMcpRuntimeConfigDTO;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：数据库内置 MCP 服务定义
 */
@Component
public class DatabaseMcpBuiltinDefinition implements McpBuiltinServerDefinition {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private McpInvocationLogManager mcpInvocationLogManager;

    @Override
    public String getBuiltinServerKey() {
        return McpManagementConstants.BUILTIN_SERVER_DATABASE;
    }

    @Override
    public McpCatalogResponse buildCatalog() {
        return McpAssembler.toCatalogResponse(
                getBuiltinServerKey(),
                "数据库只读 MCP",
                "提供白名单表结构查询和只读 SQL 查询能力，适合企业 Agent 做安全数据访问。",
                McpManagementConstants.SERVER_TYPE_BUILTIN,
                McpManagementConstants.TRANSPORT_TYPE_IN_PROCESS,
                McpManagementConstants.RISK_LEVEL_HIGH,
                0,
                List.of("mcp_query_<serverCode>", "mcp_schema_<serverCode>"),
                toJson(DatabaseMcpRuntimeConfigDTO.builder()
                        .dataSourceBeanName("dataSource")
                        .defaultLimit(20)
                        .maxLimit(200)
                        .queryTimeoutSeconds(10)
                        .allowSchemaInspect(1)
                        .allowedTables(List.of("orders", "users"))
                        .build()),
                null,
                """
                        {"mode":"QUERY","sql":"select * from orders where status = 'PAID'","limit":10}
                        """,
                "先调用 schema 工具了解字段，再发起只读 SQL 查询。禁止拼接写操作语句。"
        );
    }

    @Override
    public List<ToolCallback> createToolCallbacks(McpServerRecord record, McpServerExtDTO ext) {
        DatabaseMcpRuntimeConfigDTO runtimeConfig = parseRuntimeConfig(ext);
        DataSource dataSource = resolveDataSource(runtimeConfig);
        DatabaseQueryTool queryTool = new DatabaseQueryTool(record, ext, runtimeConfig, dataSource);
        DatabaseSchemaTool schemaTool = new DatabaseSchemaTool(record, ext, runtimeConfig, dataSource);
        ToolCallback queryCallback = FunctionToolCallback.builder(queryTool.buildToolName(), (DatabaseQueryTool.DatabaseQueryRequest request, ToolContext toolContext) ->
                        invokeWithRuntimeLog(
                                record,
                                queryTool.buildToolName(),
                                request,
                                () -> queryTool.execute(request).getDisplayText()))
                .description(queryTool.buildDescription())
                .inputType(DatabaseQueryTool.DatabaseQueryRequest.class)
                .build();
        if (!Integer.valueOf(1).equals(runtimeConfig.getAllowSchemaInspect())) {
            return List.of(queryCallback);
        }
        ToolCallback schemaCallback = FunctionToolCallback.builder(schemaTool.buildToolName(), (DatabaseSchemaTool.DatabaseSchemaRequest request, ToolContext toolContext) ->
                        invokeWithRuntimeLog(
                                record,
                                schemaTool.buildToolName(),
                                request,
                                () -> schemaTool.execute(request).getDisplayText()))
                .description(schemaTool.buildDescription())
                .inputType(DatabaseSchemaTool.DatabaseSchemaRequest.class)
                .build();
        return List.of(queryCallback, schemaCallback);
    }

    @Override
    public McpInvocationResult debugInvoke(McpServerRecord record, McpServerExtDTO ext, McpAuthConfigDTO authConfig, String requestPayloadJson) {
        DatabaseMcpRuntimeConfigDTO runtimeConfig = parseRuntimeConfig(ext);
        DataSource dataSource = resolveDataSource(runtimeConfig);
        String normalizedPayload = StringUtils.hasText(requestPayloadJson) ? requestPayloadJson.trim() : "{}";
        try {
            DebugRequest debugRequest = objectMapper.readValue(normalizedPayload, DebugRequest.class);
            if (McpManagementConstants.TOOL_MODE_SCHEMA.equalsIgnoreCase(debugRequest.getMode())) {
                DatabaseSchemaTool schemaTool = new DatabaseSchemaTool(record, ext, runtimeConfig, dataSource);
                DatabaseSchemaTool.DatabaseSchemaRequest request = new DatabaseSchemaTool.DatabaseSchemaRequest();
                request.tableName = debugRequest.getTableName();
                return schemaTool.execute(request);
            }
            DatabaseQueryTool queryTool = new DatabaseQueryTool(record, ext, runtimeConfig, dataSource);
            DatabaseQueryTool.DatabaseQueryRequest request = new DatabaseQueryTool.DatabaseQueryRequest();
            request.sql = debugRequest.getSql();
            request.limit = debugRequest.getLimit();
            request.purpose = "debug";
            return queryTool.execute(request);
        } catch (Exception exception) {
            throw new IllegalStateException("数据库 MCP 调试失败: " + exception.getMessage(), exception);
        }
    }

    private DatabaseMcpRuntimeConfigDTO parseRuntimeConfig(McpServerExtDTO ext) {
        try {
            if (!StringUtils.hasText(ext.getRuntimeConfigJson())) {
                return DatabaseMcpRuntimeConfigDTO.builder()
                        .dataSourceBeanName("dataSource")
                        .defaultLimit(20)
                        .maxLimit(200)
                        .queryTimeoutSeconds(10)
                        .allowSchemaInspect(1)
                        .allowedTables(List.of())
                        .build();
            }
            return objectMapper.readValue(ext.getRuntimeConfigJson(), DatabaseMcpRuntimeConfigDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("数据库 MCP 运行时配置解析失败: " + exception.getMessage(), exception);
        }
    }

    private DataSource resolveDataSource(DatabaseMcpRuntimeConfigDTO runtimeConfig) {
        if (runtimeConfig != null && StringUtils.hasText(runtimeConfig.getDataSourceBeanName())) {
            return applicationContext.getBean(runtimeConfig.getDataSourceBeanName().trim(), DataSource.class);
        }
        return applicationContext.getBean(DataSource.class);
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

    public static class DebugRequest {

        private String mode;

        private String sql;

        private Integer limit;

        private String tableName;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
}
