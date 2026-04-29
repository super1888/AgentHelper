package com.spring.ai.mcp.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.common.repository.service.McpExecutionLogRecordService;
import com.spring.ai.common.repository.service.McpServerRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.mcp.config.McpManagementConstants;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 模块公共支撑组件
 * 核心职责：处理当前用户上下文、服务归属校验和扩展 JSON 读写
 */
@Component
public class McpSupportManager {

    private static final TypeReference<McpServerExtDTO> EXT_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private McpServerRecordService mcpServerRecordService;

    @Resource
    private McpExecutionLogRecordService mcpExecutionLogRecordService;

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
     * 校验并返回当前租户下的 MCP 服务记录。
     */
    public McpServerRecord requireServer(Long serverId) {
        McpServerRecord record = mcpServerRecordService.getById(serverId);
        if (record == null || !getCurrentTenantId().equals(record.getTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("未找到 MCP 服务: " + serverId);
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
     * 解析 MCP 扩展配置 JSON。
     */
    public McpServerExtDTO parseExt(String json) {
        if (!StringUtils.hasText(json)) {
            return emptyExt();
        }
        try {
            McpServerExtDTO ext = objectMapper.readValue(json, EXT_TYPE);
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
     * 构建 MCP 扩展配置 JSON。
     */
    public String buildExtJson(McpServerExtDTO ext) {
        McpServerExtDTO normalizedExt = ext == null ? emptyExt() : ext;
        if (normalizedExt.getTags() == null) {
            normalizedExt.setTags(Collections.emptyList());
        }
        return toJson(normalizedExt);
    }

    /**
     * 解析鉴权配置 JSON。
     */
    public McpAuthConfigDTO parseAuthConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new McpAuthConfigDTO();
        }
        try {
            return objectMapper.readValue(json, McpAuthConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "鉴权配置 JSON 解析失败",
                    e);
        }
    }

    /**
     * 统计指定服务的日志数量。
     */
    public Integer countLogs(Long serverId) {
        return mcpExecutionLogRecordService.listByServerId(serverId, getCurrentTenantId()).size();
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

    /**
     * 将字符串主键安全转换为 Long。
     */
    public List<Long> parseLongIds(List<String> rawIds, String fieldName) {
        if (CollectionUtils.isEmpty(rawIds)) {
            return List.of();
        }
        return rawIds.stream()
                .map(rawId -> parseLongId(rawId, fieldName))
                .distinct()
                .toList();
    }

    /**
     * 将字符串主键安全转换为 Long。
     */
    public Long parseLongId(String rawId, String fieldName) {
        if (!StringUtils.hasText(rawId)) {
            throw BusinessExceptions.badRequest(fieldName + " 不能为空");
        }
        String normalizedId = rawId.trim();
        if (!normalizedId.matches("\\d+")) {
            throw BusinessExceptions.badRequest(fieldName + " 必须为数字字符串");
        }
        try {
            return Long.valueOf(normalizedId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 超出 Long 范围",
                    exception);
        }
    }

    /**
     * 提取 JSON 指定路径的节点文本。
     */
    public String extractJsonPathText(String jsonText, String path) {
        if (!StringUtils.hasText(jsonText) || !StringUtils.hasText(path)) {
            return jsonText;
        }
        try {
            JsonNode current = objectMapper.readTree(jsonText);
            String[] pathItems = path.trim().split("\\.");
            for (String pathItem : pathItems) {
                if (current == null || !StringUtils.hasText(pathItem)) {
                    return jsonText;
                }
                current = current.get(pathItem.trim());
            }
            return current == null ? jsonText : toJson(current);
        } catch (Exception exception) {
            return jsonText;
        }
    }

    /**
     * 校验 MCP 服务是否允许挂载到 Agent。
     */
    public void validateBindableServer(McpServerRecord record) {
        if (record == null) {
            throw BusinessExceptions.notFound("未找到 MCP 服务");
        }
        if (!McpManagementConstants.SERVER_STATUS_ENABLED.equals(record.getServerStatus())) {
            throw BusinessExceptions.badRequest("MCP 服务未启用: " + record.getServerCode());
        }
        if (!McpManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(record.getPublishStatus())) {
            throw BusinessExceptions.badRequest("MCP 服务未发布: " + record.getServerCode());
        }
    }

    private McpServerExtDTO emptyExt() {
        return McpServerExtDTO.builder()
                .tags(Collections.emptyList())
                .runtimeConfigJson(null)
                .authConfigJson(null)
                .testPayloadJson(null)
                .toolPromptHint(null)
                .build();
    }
}
