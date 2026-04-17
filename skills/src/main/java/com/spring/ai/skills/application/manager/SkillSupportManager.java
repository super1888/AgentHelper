package com.spring.ai.skills.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.service.SkillRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 文件用途：Skill 管理公共支撑组件
 * 作者：Codex
 * 创建时间：2026-04-17
 * 核心功能：提供当前用户/租户委托能力、Skill 权限校验以及 JSON 序列化与反序列化能力
 */
@Component
public class SkillSupportManager {

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private SkillRecordService skillRecordService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取当前登录用户 ID。
     */
    public Long getCurrentUserId() {
        return currentUserContextSupport.getCurrentUserId();
    }

    /**
     * 获取当前登录用户名。
     */
    public String getCurrentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    /**
     * 获取当前用户所属租户 ID。
     */
    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantId();
    }

    /**
     * 按租户边界校验并返回 Skill。
     */
    public SkillRecord requireSkill(Long skillId) {
        SkillRecord record = skillRecordService.getById(skillId);
        if (record == null || !getCurrentTenantId().equals(record.getTenantId())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到对应的 Skill: " + skillId);
        }
        return record;
    }

    /**
     * 对对象执行 JSON 序列化。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON 序列化失败", e);
        }
    }

    /**
     * 把持久化快照解析为 Skill 配置对象。
     */
    public SkillSnapshotDTO parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return SkillSnapshotDTO.builder().build();
        }
        try {
            return objectMapper.readValue(json, SkillSnapshotDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Skill 配置解析失败", e);
        }
    }
}
