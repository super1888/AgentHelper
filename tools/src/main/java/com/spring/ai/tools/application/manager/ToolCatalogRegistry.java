package com.spring.ai.tools.application.manager;

import com.spring.ai.tools.application.assmbler.ToolAssembler;
import com.spring.ai.tools.config.ToolManagementConstants;
import com.spring.ai.tools.domain.response.ToolCatalogResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 文件用途：内置工具目录注册表
 * 核心功能：提供工具管理页选择的内置工具模板和默认配置
 */
@Component
public class ToolCatalogRegistry {

    /**
     * 返回当前系统内置工具目录。
     */
    public List<ToolCatalogResponse> listCatalog() {
        return List.of(
                ToolAssembler.toCatalogResponse(
                        "calculator-tools",
                        "Calculator Tools",
                        "执行简单数值计算与数学辅助",
                        "CALCULATION",
                        "COMPUTE",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "math"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"a\": {\"type\": \"number\"},\n    \"b\": {\"type\": \"number\"}\n  }\n}",
                        "{\n  \"method\": \"add\"\n}",
                        "{\n  \"a\": 12,\n  \"b\": 8\n}"
                ),
                ToolAssembler.toCatalogResponse(
                        "date-time-tools",
                        "Date Time Tools",
                        "提供时间与日期获取能力",
                        "DATETIME",
                        "SYSTEM",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "time"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {}\n}",
                        "{\n  \"timezone\": \"Asia/Shanghai\"\n}",
                        "{}"
                ),
                ToolAssembler.toCatalogResponse(
                        "web-search-tool",
                        "Web Search Tool",
                        "执行联网搜索与结果摘要",
                        "SEARCH",
                        "KNOWLEDGE",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "search"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"query\": {\"type\": \"string\"}\n  }\n}",
                        "{\n  \"topK\": 5,\n  \"timeoutSeconds\": 10\n}",
                        "{\n  \"query\": \"Spring AI latest release\"\n}"
                ),
                ToolAssembler.toCatalogResponse(
                        "document-search-tool",
                        "Document Search Tool",
                        "执行文档检索和摘要抽取",
                        "DOCUMENT",
                        "KNOWLEDGE",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "document"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"keyword\": {\"type\": \"string\"}\n  }\n}",
                        "{\n  \"rootPath\": \"docs\",\n  \"topK\": 5\n}",
                        "{\n  \"keyword\": \"工具管理\"\n}"
                ),
                ToolAssembler.toCatalogResponse(
                        "database-query-tools",
                        "Database Query Tools",
                        "执行数据库查询与结果预览",
                        "DATABASE",
                        "DATA",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "database"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"sql\": {\"type\": \"string\"}\n  }\n}",
                        "{\n  \"readonly\": true,\n  \"maxRows\": 100\n}",
                        "{\n  \"sql\": \"select * from tool_record limit 10\"\n}"
                ),
                ToolAssembler.toCatalogResponse(
                        "python-tool",
                        "Python Tool",
                        "执行受控 Python 代码片段",
                        "SCRIPT",
                        "AUTOMATION",
                        ToolManagementConstants.SOURCE_TYPE_BUILTIN,
                        List.of("builtin", "python"),
                        "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"code\": {\"type\": \"string\"}\n  }\n}",
                        "{\n  \"sandbox\": true,\n  \"timeoutSeconds\": 15\n}",
                        "{\n  \"code\": \"print(1 + 1)\"\n}"
                )
        );
    }
}
