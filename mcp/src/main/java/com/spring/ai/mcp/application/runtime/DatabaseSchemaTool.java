package com.spring.ai.mcp.application.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.domain.dto.DatabaseMcpRuntimeConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 文件用途：数据库表结构查询 MCP 工具
 * 核心职责：按白名单输出表字段信息，帮助 Agent 在执行 SQL 前理解可查询结构
 */
public class DatabaseSchemaTool {

    private final McpServerRecord record;
    private final McpServerExtDTO ext;
    private final DatabaseMcpRuntimeConfigDTO runtimeConfig;
    private final DataSource dataSource;

    public DatabaseSchemaTool(
            McpServerRecord record,
            McpServerExtDTO ext,
            DatabaseMcpRuntimeConfigDTO runtimeConfig,
            DataSource dataSource
    ) {
        this.record = record;
        this.ext = ext;
        this.runtimeConfig = runtimeConfig;
        this.dataSource = dataSource;
    }

    /**
     * 执行表结构查询。
     */
    public McpInvocationResult execute(DatabaseSchemaRequest request) {
        String tableName = normalizeTableName(request == null ? null : request.tableName);
        List<String> rows = new ArrayList<>();
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
                while (resultSet.next()) {
                    rows.add("%s %s nullable=%s remarks=%s".formatted(
                            resultSet.getString("COLUMN_NAME"),
                            resultSet.getString("TYPE_NAME"),
                            resultSet.getString("IS_NULLABLE"),
                            resultSet.getString("REMARKS")));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("读取数据库表结构失败: " + exception.getMessage(), exception);
        }
        String responseText = rows.isEmpty()
                ? "未读取到表结构，请确认表名和白名单配置: " + tableName
                : "表 " + tableName + " 字段如下:\n" + String.join("\n", rows);
        return McpInvocationResult.builder()
                .toolName(buildToolName())
                .displayText(responseText)
                .responsePayloadJson("""
                        {"tableName":%s,"serverCode":%s,"columns":%s}
                        """.formatted(
                        quote(tableName),
                        quote(record.getServerCode()),
                        toJsonArray(rows)))
                .build();
    }

    /**
     * 暴露给 Agent 的方法签名。
     */
    public String apply(DatabaseSchemaRequest request, ToolContext toolContext) {
        return execute(request).getDisplayText();
    }

    public String buildToolName() {
        return "mcp_schema_" + sanitize(record.getServerCode());
    }

    public String buildDescription() {
        return "读取数据库表结构信息。仅允许查询白名单中的表。服务编码: " + record.getServerCode()
                + buildPromptHintSuffix();
    }

    private String normalizeTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        String normalizedTableName = tableName.trim();
        List<String> allowedTables = runtimeConfig == null ? List.of() : runtimeConfig.getAllowedTables();
        if (!CollectionUtils.isEmpty(allowedTables) && allowedTables.stream().noneMatch(item -> normalizedTableName.equalsIgnoreCase(item))) {
            throw new IllegalArgumentException("当前 MCP 服务未授权访问表: " + normalizedTableName);
        }
        return normalizedTableName;
    }

    private String buildPromptHintSuffix() {
        return StringUtils.hasText(ext.getToolPromptHint()) ? "。补充提示: " + ext.getToolPromptHint().trim() : "";
    }

    private String sanitize(String value) {
        return value == null ? "server" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private String toJsonArray(List<String> rows) {
        return "[" + rows.stream().map(this::quote).reduce((left, right) -> left + "," + right).orElse("") + "]";
    }

    public static class DatabaseSchemaRequest {

        @JsonProperty(required = true)
        @JsonPropertyDescription("需要查看结构的表名，例如 orders")
        public String tableName;
    }
}
