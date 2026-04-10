package com.spring.ai.tools.custom;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Database query tool.
 *
 * <p>This is a placeholder implementation so the agent can already call a
 * database tool entry point. Later this can be wired to JdbcTemplate, MyBatis,
 * or a business repository.</p>
 *
 * @author zhouqi
 * @since 2026/4/10
 */
public class DatabaseQueryTools {

    /**
     * 从数据库中查询业务数据。当前版本为代理集成的占位符
     * @param question
     * @param tableName
     * @param conditions
     * @param limit
     * @param toolContext
     * @return
     */
    //
    @Tool(description = "Query business data from a database. Current version is a placeholder for agent integration")
    public String queryDatabase(
            @ToolParam(description = "Business question such as query order data in the last 7 days") String question,
            @ToolParam(description = "Table name, optional for now, such as orders or users") String tableName,
            @ToolParam(description = "Filter conditions, for example status=paid,userId=1") String conditions,
            @ToolParam(description = "Max row count, default is 10") Integer limit,
            ToolContext toolContext) {

        String normalizedQuestion = question == null ? "" : question.trim();
        String normalizedTable = tableName == null || tableName.trim().isEmpty() ? "unknown_table" : tableName.trim();
        String normalizedConditions = conditions == null || conditions.trim().isEmpty() ? "1=1" : conditions.trim();
        int resultLimit = normalizeLimit(limit);

        return """
                Database query tool is currently a placeholder and is not connected to a real database.
                User question: %s
                Target table: %s
                Conditions: %s
                Limit: %s
                Suggested SQL:
                SELECT * FROM %s WHERE %s LIMIT %s;

                Next integration steps:
                1. Inject JdbcTemplate, Mapper, or Repository here
                2. Add whitelist validation for table names and fields
                3. Convert natural language into structured query parameters
                4. Format database rows before returning them to the agent

                ToolContext: %s
                """.formatted(
                normalizedQuestion.isEmpty() ? "No question provided" : normalizedQuestion,
                normalizedTable,
                normalizedConditions,
                resultLimit,
                normalizedTable,
                normalizedConditions,
                resultLimit,
                toolContext == null ? "none" : "available");
    }

    /**
     * 描述当前数据库工具的功能，以便代理能够决定何时使用它
     * @return
     */
    @Tool(description = "Describe current database tool capability so the agent can decide when to use it")
    public String getDatabaseToolCapability() {
        return """
                Current database tool capability:
                1. Agent-callable entry point is ready
                2. Basic query arguments are defined
                3. Real database connection is not wired yet
                4. SQL generation, permission control, and mapping are not implemented yet

                Recommended next steps:
                1. Start with JdbcTemplate for a few fixed tables
                2. Add whitelist validation and permission control
                3. Then support natural-language-to-SQL conversion
                """;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
