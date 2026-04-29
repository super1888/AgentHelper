package com.spring.ai.mcp.application.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.domain.dto.DatabaseMcpRuntimeConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 文件用途：数据库只读查询 MCP 工具
 * 核心职责：对 Agent 暴露受限 SQL 查询能力，并在运行时执行白名单和只读校验
 */
public class DatabaseQueryTool {

    private static final Pattern FROM_OR_JOIN_PATTERN = Pattern.compile("\\b(?:from|join)\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);

    private final McpServerRecord record;
    private final McpServerExtDTO ext;
    private final DatabaseMcpRuntimeConfigDTO runtimeConfig;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseQueryTool(
            McpServerRecord record,
            McpServerExtDTO ext,
            DatabaseMcpRuntimeConfigDTO runtimeConfig,
            DataSource dataSource
    ) {
        this.record = record;
        this.ext = ext;
        this.runtimeConfig = runtimeConfig;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        if (runtimeConfig != null && runtimeConfig.getQueryTimeoutSeconds() != null && runtimeConfig.getQueryTimeoutSeconds() > 0) {
            this.jdbcTemplate.setQueryTimeout(runtimeConfig.getQueryTimeoutSeconds());
        }
    }

    /**
     * 执行数据库查询。
     */
    public McpInvocationResult execute(DatabaseQueryRequest request) {
        String sql = normalizeSql(request == null ? null : request.sql);
        int limit = normalizeLimit(request == null ? null : request.limit);
        validateSql(sql);
        String finalSql = appendLimitIfAbsent(sql, limit);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(finalSql);
        String responsePayloadJson = rowsToJson(rows);
        return McpInvocationResult.builder()
                .toolName(buildToolName())
                .displayText(responsePayloadJson)
                .responsePayloadJson("""
                        {"serverCode":%s,"rowCount":%s,"rows":%s}
                        """.formatted(quote(record.getServerCode()), rows.size(), responsePayloadJson))
                .build();
    }

    /**
     * 暴露给 Agent 的工具入口。
     */
    public String apply(DatabaseQueryRequest request, ToolContext toolContext) {
        return execute(request).getDisplayText();
    }

    public String buildToolName() {
        return "mcp_query_" + sanitize(record.getServerCode());
    }

    public String buildDescription() {
        return "执行数据库只读 SQL 查询。仅允许 SELECT/WITH 且受表白名单限制。服务编码: " + record.getServerCode()
                + buildPromptHintSuffix();
    }

    private String normalizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("sql 不能为空");
        }
        return sql.trim();
    }

    private int normalizeLimit(Integer limit) {
        int defaultLimit = runtimeConfig != null && runtimeConfig.getDefaultLimit() != null && runtimeConfig.getDefaultLimit() > 0
                ? runtimeConfig.getDefaultLimit()
                : 20;
        int maxLimit = runtimeConfig != null && runtimeConfig.getMaxLimit() != null && runtimeConfig.getMaxLimit() > 0
                ? runtimeConfig.getMaxLimit()
                : 200;
        int normalizedLimit = limit == null ? defaultLimit : limit;
        if (normalizedLimit <= 0) {
            return defaultLimit;
        }
        return Math.min(normalizedLimit, maxLimit);
    }

    /**
     * 对 SQL 做只读和白名单约束，避免将 MCP 服务降级为任意数据库执行器。
     */
    private void validateSql(String sql) {
        String normalizedSql = sql.toLowerCase(Locale.ROOT);
        if (!(normalizedSql.startsWith("select") || normalizedSql.startsWith("with"))) {
            throw new IllegalArgumentException("仅允许执行 SELECT 或 WITH 查询");
        }
        if (normalizedSql.contains(";")) {
            throw new IllegalArgumentException("不允许执行多语句 SQL");
        }
        if (normalizedSql.matches(".*\\b(update|delete|insert|drop|alter|truncate|grant|revoke|create|merge|replace)\\b.*")) {
            throw new IllegalArgumentException("检测到危险 SQL 关键字，已拒绝执行");
        }
        List<String> allowedTables = runtimeConfig == null ? List.of() : runtimeConfig.getAllowedTables();
        if (CollectionUtils.isEmpty(allowedTables)) {
            return;
        }
        Matcher matcher = FROM_OR_JOIN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String pureTableName = tableName.contains(".") ? tableName.substring(tableName.lastIndexOf('.') + 1) : tableName;
            boolean matched = allowedTables.stream().anyMatch(item -> pureTableName.equalsIgnoreCase(item) || tableName.equalsIgnoreCase(item));
            if (!matched) {
                throw new IllegalArgumentException("当前 MCP 服务未授权访问表: " + pureTableName);
            }
        }
    }

    private String appendLimitIfAbsent(String sql, int limit) {
        if (sql.toLowerCase(Locale.ROOT).contains(" limit ")) {
            return sql;
        }
        return sql + " limit " + limit;
    }

    private String rowsToJson(List<Map<String, Object>> rows) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{");
            int fieldIndex = 0;
            for (Map.Entry<String, Object> entry : rows.get(i).entrySet()) {
                if (fieldIndex > 0) {
                    builder.append(",");
                }
                builder.append(quote(entry.getKey()))
                        .append(":")
                        .append(quote(entry.getValue() == null ? null : String.valueOf(entry.getValue())));
                fieldIndex++;
            }
            builder.append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private String buildPromptHintSuffix() {
        return StringUtils.hasText(ext.getToolPromptHint()) ? "。补充提示: " + ext.getToolPromptHint().trim() : "";
    }

    private String sanitize(String value) {
        return value == null ? "server" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static class DatabaseQueryRequest {

        @JsonProperty(required = true)
        @JsonPropertyDescription("只读 SQL，必须是 SELECT 或 WITH 查询")
        public String sql;

        @JsonPropertyDescription("单次查询最多返回多少行，不传则使用服务默认值")
        public Integer limit;

        @JsonPropertyDescription("本次查询的业务目的，帮助审计日志定位场景")
        public String purpose;
    }
}
