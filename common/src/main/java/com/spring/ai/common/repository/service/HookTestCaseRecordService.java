package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.HookTestCaseRecord;
import java.util.List;

/**
 * 文件用途：Hook 测试用例服务接口
 */
public interface HookTestCaseRecordService extends IService<HookTestCaseRecord> {

    /**
     * 查询 Hook 测试用例列表。
     */
    List<HookTestCaseRecord> listByHookId(Long hookId, Long tenantId);
}
