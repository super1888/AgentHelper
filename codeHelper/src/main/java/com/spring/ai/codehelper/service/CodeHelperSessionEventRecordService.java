package com.spring.ai.codehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import java.util.List;

/**
 * 代码助手会话事件 Service。
 */
public interface CodeHelperSessionEventRecordService extends IService<CodeHelperSessionEventRecord> {

    List<CodeHelperSessionEventRecord> listBySessionCode(String sessionCode, Long tenantId);

    Long nextSequence(String sessionCode, Long tenantId);
}
