package com.spring.ai.skills.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.service.SkillRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 文件用途：Skills 模块公共支撑组件
 * 核心功能：提供当前用户上下文、技能归属校验和 JSON 序列化能力
 */
@Component
public class SkillSupportManager {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private SkillRecordService skillRecordService;

    @Resource
    private ObjectMapper objectMapper;

    public Long getCurrentUserId() {
        return currentUserContextSupport.getCurrentUserId();
    }

    public String getCurrentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantId();
    }

    public SkillRecord requireSkill(Long skillId) {
        SkillRecord record = skillRecordService.getById(skillId);
        if (record == null || !Objects.equals(getCurrentTenantId(), record.getTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到对应技能：" + skillId);
        }
        return record;
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

    public SkillSnapshotDTO parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return SkillSnapshotDTO.builder().build();
        }
        try {
            return objectMapper.readValue(json, SkillSnapshotDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "技能快照解析失败", e);
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
}
