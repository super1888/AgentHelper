package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.A2aTaskRecord;
import java.util.List;

public interface A2aTaskRecordService extends IService<A2aTaskRecord> {

    List<A2aTaskRecord> listByTenantId(Long tenantId);
}
