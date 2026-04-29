package com.spring.ai.mcp.application.registry;

import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.mcp.application.model.McpBuiltinServerDefinition;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 文件用途：MCP 内置服务目录注册表
 */
@Component
public class McpCatalogRegistry {

    @Resource
    private List<McpBuiltinServerDefinition> definitions;

    /**
     * 查询内置服务目录。
     */
    public List<McpCatalogResponse> listCatalog() {
        return definitions.stream()
                .map(McpBuiltinServerDefinition::buildCatalog)
                .toList();
    }

    /**
     * 按内置服务标识查找定义。
     */
    public McpBuiltinServerDefinition require(String builtinServerKey) {
        return definitions.stream()
                .filter(item -> item.getBuiltinServerKey().equals(builtinServerKey))
                .findFirst()
                .orElseThrow(() -> BusinessExceptions.badRequest("不支持的 builtinServerKey: " + builtinServerKey));
    }
}
