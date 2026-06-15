package com.spring.ai.codehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.codehelper.domain.entity.CodeHelperToolLogRecord;
import java.util.List;

/**
 * 代码助手工具日志 Service。
 */
public interface CodeHelperToolLogRecordService extends IService<CodeHelperToolLogRecord> {

    List<CodeHelperToolLogRecord> listBySessionCode(String sessionCode, Long tenantId);

    List<CodeHelperToolLogRecord> listByTenantId(Long tenantId);
}
