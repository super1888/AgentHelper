package com.spring.ai.interceptors.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.service.InterceptorAgentBindingRecordService;
import com.spring.ai.common.repository.service.InterceptorExecutionLogRecordService;
import com.spring.ai.common.repository.service.InterceptorRecordService;
import com.spring.ai.common.repository.service.InterceptorTestCaseRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.interceptors.domain.dto.InterceptorSnapshotDTO;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Support utilities for interceptor management.
 */
@Component
public class InterceptorSupportManager {

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private InterceptorRecordService interceptorRecordService;

    @Resource
    private InterceptorAgentBindingRecordService interceptorAgentBindingRecordService;

    @Resource
    private InterceptorTestCaseRecordService interceptorTestCaseRecordService;

    @Resource
    private InterceptorExecutionLogRecordService interceptorExecutionLogRecordService;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    public Long getCurrentUserId() {
        return currentUserContextSupport.getCurrentUserId();
    }

    public String getCurrentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    public InterceptorRecord requireInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorRecordService.getById(interceptorId);
        if (record == null || !Objects.equals(record.getTenantId(), getCurrentTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "Interceptor not found: " + interceptorId);
        }
        return record;
    }

    public InterceptorSnapshotDTO parseSnapshot(String json) {
        InterceptorSnapshotDTO snapshot = commonJsonUtils.parseObject(json, InterceptorSnapshotDTO.class);
        return snapshot == null ? InterceptorSnapshotDTO.builder().build() : snapshot;
    }

    public Map<String, Object> parseMap(String json) {
        return commonJsonUtils.parseMap(json);
    }

    public String toJson(Object value) {
        return commonJsonUtils.toJson(value);
    }

    public String prettyJson(Object value) {
        return commonJsonUtils.prettyJson(value);
    }

    public Integer countBindings(Long interceptorId) {
        return interceptorAgentBindingRecordService.listByInterceptorId(interceptorId, getCurrentTenantId()).size();
    }

    public Integer countTestCases(Long interceptorId) {
        return interceptorTestCaseRecordService.listByInterceptorId(interceptorId, getCurrentTenantId()).size();
    }

    public Integer countLogs(Long interceptorId) {
        return interceptorExecutionLogRecordService.listByCondition(getCurrentTenantId(), interceptorId, null, null).size();
    }
}
