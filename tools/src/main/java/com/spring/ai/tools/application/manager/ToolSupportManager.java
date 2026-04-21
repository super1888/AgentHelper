package com.spring.ai.tools.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.ToolRecord;
import com.spring.ai.common.repository.service.ToolExecutionLogRecordService;
import com.spring.ai.common.repository.service.ToolRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.tools.domain.dto.ToolRecordExtDTO;
import jakarta.annotation.Resource;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：工具管理公共支撑组件
 * 核心功能：处理当前用户上下文、工具归属校验和工具扩展 JSON 的读写
 */
@Component
public class ToolSupportManager {

    private static final TypeReference<ToolRecordExtDTO> EXT_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private ToolRecordService toolRecordService;

    @Resource
    private ToolExecutionLogRecordService toolExecutionLogRecordService;

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
     * 获取当前租户 ID。
     */
    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    /**
     * 校验并返回当前租户下的工具记录。
     */
    public ToolRecord requireTool(Long toolId) {
        ToolRecord record = toolRecordService.getById(toolId);
        if (record == null || !getCurrentTenantId().equals(record.getTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到工具: " + toolId);
        }
        return record;
    }

    /**
     * 将对象统一序列化为 JSON。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 序列化失败",
                    e);
        }
    }

    /**
     * 解析工具扩展配置 JSON。
     */
    public ToolRecordExtDTO parseExt(String json) {
        if (!StringUtils.hasText(json)) {
            return emptyExt();
        }
        try {
            ToolRecordExtDTO ext = objectMapper.readValue(json, EXT_TYPE);
            if (ext == null) {
                return emptyExt();
            }
            if (ext.getTags() == null) {
                ext.setTags(Collections.emptyList());
            }
            return ext;
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 解析失败",
                    e);
        }
    }

    /**
     * 构建工具扩展配置 JSON。
     */
    public String buildExtJson(ToolRecordExtDTO ext) {
        ToolRecordExtDTO normalizedExt = ext == null ? emptyExt() : ext;
        if (normalizedExt.getTags() == null) {
            normalizedExt.setTags(Collections.emptyList());
        }
        return toJson(normalizedExt);
    }

    /**
     * 统计指定工具的日志数量。
     */
    public Integer countLogs(Long toolId) {
        return toolExecutionLogRecordService.listByToolId(toolId, getCurrentTenantId()).size();
    }

    /**
     * 校验 JSON 文本是否合法。
     */
    public void validateJsonText(String jsonText, String fieldName) {
        if (!StringUtils.hasText(jsonText)) {
            return;
        }
        try {
            objectMapper.readTree(jsonText);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 不是合法 JSON",
                    e);
        }
    }

    private ToolRecordExtDTO emptyExt() {
        return ToolRecordExtDTO.builder()
                .tags(Collections.emptyList())
                .requestSchemaJson(null)
                .authConfigJson(null)
                .runtimeConfigJson(null)
                .testPayloadJson(null)
                .build();
    }
}
