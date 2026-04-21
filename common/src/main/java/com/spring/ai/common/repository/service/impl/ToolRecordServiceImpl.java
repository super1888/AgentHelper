package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.ToolRecordMapper;
import com.spring.ai.common.repository.enitiy.ToolRecord;
import com.spring.ai.common.repository.service.ToolRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：工具管理主表服务实现
 */
@Service
public class ToolRecordServiceImpl extends ServiceImpl<ToolRecordMapper, ToolRecord> implements ToolRecordService {

    /**
     * 查询当前租户下未删除的工具列表。
     */
    @Override
    public List<ToolRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(ToolRecord.class)
                .eq(ToolRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(ToolRecord::getDeletedFlag, 1).or().isNull(ToolRecord::getDeletedFlag))
                .orderByDesc(ToolRecord::getSortWeight)
                .orderByDesc(ToolRecord::getUpdateTime)
                .orderByDesc(ToolRecord::getId));
    }

    /**
     * 按工具编码查询工具记录。
     */
    @Override
    public ToolRecord getByToolCode(Long tenantId, String toolCode) {
        if (tenantId == null || !StringUtils.hasText(toolCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(ToolRecord.class)
                .eq(ToolRecord::getTenantId, tenantId)
                .eq(ToolRecord::getToolCode, toolCode.trim())
                .and(wrapper -> wrapper.ne(ToolRecord::getDeletedFlag, 1).or().isNull(ToolRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
