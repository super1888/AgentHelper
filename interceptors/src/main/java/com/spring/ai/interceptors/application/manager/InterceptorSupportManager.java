package com.spring.ai.interceptors.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.service.InterceptorAgentBindingRecordService;
import com.spring.ai.common.repository.service.InterceptorExecutionLogRecordService;
import com.spring.ai.common.repository.service.InterceptorRecordService;
import com.spring.ai.common.repository.service.InterceptorTestCaseRecordService;
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

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

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
    private ObjectMapper objectMapper;

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
        if (json == null || json.isBlank()) {
            return InterceptorSnapshotDTO.builder().build();
        }
        try {
            return objectMapper.readValue(json, InterceptorSnapshotDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Interceptor snapshot parse failed",
                    e
            );
        }
    }

    public Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "JSON parse failed", e);
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON serialize failed",
                    e
            );
        }
    }

    public String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON pretty print failed",
                    e
            );
        }
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
