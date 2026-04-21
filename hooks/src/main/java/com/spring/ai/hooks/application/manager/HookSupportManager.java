package com.spring.ai.hooks.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.HookRecord;
import com.spring.ai.common.repository.service.HookAgentBindingRecordService;
import com.spring.ai.common.repository.service.HookExecutionLogRecordService;
import com.spring.ai.common.repository.service.HookRecordService;
import com.spring.ai.common.repository.service.HookTestCaseRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.hooks.domain.dto.HookSnapshotDTO;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 文件用途：Hook 模块公共支撑组件
 * 核心职责：处理当前用户上下文、Hook 归属校验和 JSON 能力
 */
@Component
public class HookSupportManager {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private HookRecordService hookRecordService;

    @Resource
    private HookAgentBindingRecordService hookAgentBindingRecordService;

    @Resource
    private HookTestCaseRecordService hookTestCaseRecordService;

    @Resource
    private HookExecutionLogRecordService hookExecutionLogRecordService;

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

    /**
     * 校验并返回当前租户下的 Hook。
     */
    public HookRecord requireHook(Long hookId) {
        HookRecord record = hookRecordService.getById(hookId);
        if (record == null || !Objects.equals(record.getTenantId(), getCurrentTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到 Hook：" + hookId);
        }
        return record;
    }

    public HookSnapshotDTO parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return HookSnapshotDTO.builder().build();
        }
        try {
            return objectMapper.readValue(json, HookSnapshotDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Hook 快照解析失败", e);
        }
    }

    public Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON 解析失败", e);
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON 序列化失败", e);
        }
    }

    public String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON 格式化失败", e);
        }
    }

    public Integer countBindings(Long hookId) {
        return hookAgentBindingRecordService.listByHookId(hookId, getCurrentTenantId()).size();
    }

    public Integer countTestCases(Long hookId) {
        return hookTestCaseRecordService.listByHookId(hookId, getCurrentTenantId()).size();
    }

    public Integer countLogs(Long hookId) {
        return hookExecutionLogRecordService.listByCondition(getCurrentTenantId(), hookId, null, null).size();
    }
}
