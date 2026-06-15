package com.spring.ai.codehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;

/**
 * 代码助手会话 Service。
 */
public interface CodeHelperSessionRecordService extends IService<CodeHelperSessionRecord> {

    CodeHelperSessionRecord getBySessionCode(String sessionCode, Long tenantId);
}
