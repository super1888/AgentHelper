package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import java.util.List;

/**
 * 文件用途：MCP 服务主表服务接口
 */
public interface McpServerRecordService extends IService<McpServerRecord> {

    /**
     * 查询当前租户下未删除的 MCP 服务列表。
     */
    List<McpServerRecord> listByTenantId(Long tenantId);

    /**
     * 按服务编码查询 MCP 服务记录。
     */
    McpServerRecord getByServerCode(Long tenantId, String serverCode);
}
