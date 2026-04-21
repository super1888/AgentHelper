package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.ToolRecord;
import java.util.List;

/**
 * 文件用途：工具管理主表服务接口
 */
public interface ToolRecordService extends IService<ToolRecord> {

    /**
     * 查询当前租户下未删除的工具列表。
     */
    List<ToolRecord> listByTenantId(Long tenantId);

    /**
     * 按工具编码查询工具记录。
     */
    ToolRecord getByToolCode(Long tenantId, String toolCode);
}
