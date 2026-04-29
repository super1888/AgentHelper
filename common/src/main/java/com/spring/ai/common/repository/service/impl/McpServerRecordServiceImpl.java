package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.McpServerRecordMapper;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.common.repository.service.McpServerRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 服务主表服务实现
 */
@Service
public class McpServerRecordServiceImpl extends ServiceImpl<McpServerRecordMapper, McpServerRecord> implements McpServerRecordService {

    /**
     * 查询当前租户下未删除的 MCP 服务列表。
     */
    @Override
    public List<McpServerRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(McpServerRecord.class)
                .eq(McpServerRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(McpServerRecord::getDeletedFlag, 1).or().isNull(McpServerRecord::getDeletedFlag))
                .orderByDesc(McpServerRecord::getSortWeight)
                .orderByDesc(McpServerRecord::getUpdateTime)
                .orderByDesc(McpServerRecord::getId));
    }

    /**
     * 按服务编码查询 MCP 服务记录。
     */
    @Override
    public McpServerRecord getByServerCode(Long tenantId, String serverCode) {
        if (tenantId == null || !StringUtils.hasText(serverCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(McpServerRecord.class)
                .eq(McpServerRecord::getTenantId, tenantId)
                .eq(McpServerRecord::getServerCode, serverCode.trim())
                .and(wrapper -> wrapper.ne(McpServerRecord::getDeletedFlag, 1).or().isNull(McpServerRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
