package com.spring.ai.mcp.application.model;

import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * 文件用途：内置 MCP 服务定义接口
 */
public interface McpBuiltinServerDefinition {

    /**
     * 返回内置服务唯一标识。
     */
    String getBuiltinServerKey();

    /**
     * 返回目录展示信息。
     */
    McpCatalogResponse buildCatalog();

    /**
     * 构建可挂载到 Agent 的 ToolCallback 集合。
     */
    List<ToolCallback> createToolCallbacks(McpServerRecord record, McpServerExtDTO ext);

    /**
     * 执行调试请求。
     */
    McpInvocationResult debugInvoke(McpServerRecord record, McpServerExtDTO ext, McpAuthConfigDTO authConfig, String requestPayloadJson);
}
