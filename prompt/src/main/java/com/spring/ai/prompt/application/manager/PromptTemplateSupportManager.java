package com.spring.ai.prompt.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.repository.service.PromptTemplateRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateExtDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：提示词模板公共支撑组件
 * 作者：Codex
 * 创建时间：2026-04-17
 * 核心功能：委托统一用户租户上下文，并处理模板资源校验与 JSON 解析
 */
@Component
public class PromptTemplateSupportManager {

    private static final TypeReference<List<PromptTemplateVariableDTO>> VARIABLE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<PromptTemplateExtDTO> EXT_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private PromptTemplateRecordService promptTemplateRecordService;

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
        return currentUserContextSupport.getCurrentTenantId();
    }

    /**
     * 按租户边界校验并返回模板记录。
     */
    public PromptTemplateRecord requirePromptTemplate(Long promptTemplateId) {
        PromptTemplateRecord record = promptTemplateRecordService.getById(promptTemplateId);
        if (record == null || !getCurrentTenantId().equals(record.getTenantId())) {
            throw new BusinessException(
                    ErrorCodeEnum.NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "未找到提示词模板: " + promptTemplateId);
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
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 序列化失败",
                    e);
        }
    }

    /**
     * 解析模板变量定义 JSON。
     */
    public List<PromptTemplateVariableDTO> parseVariableDefinitions(String json) {
        return parseTemplateExt(json).getVariableDefinitions();
    }

    /**
     * 解析模板扩展配置，兼容旧版变量数组结构。
     */
    public PromptTemplateExtDTO parseTemplateExt(String json) {
        if (!StringUtils.hasText(json)) {
            return emptyExt();
        }
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            if (rootNode == null || rootNode.isNull()) {
                return emptyExt();
            }
            if (rootNode.isArray()) {
                return PromptTemplateExtDTO.builder()
                        .variableDefinitions(objectMapper.convertValue(rootNode, VARIABLE_LIST_TYPE))
                        .enterpriseConfig(null)
                        .build();
            }
            PromptTemplateExtDTO ext = objectMapper.convertValue(rootNode, EXT_TYPE);
            if (ext == null) {
                return emptyExt();
            }
            if (ext.getVariableDefinitions() == null) {
                ext.setVariableDefinitions(Collections.emptyList());
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
     * 统一序列化模板扩展配置。
     */
    public String buildTemplateExtJson(
            List<PromptTemplateVariableDTO> variableDefinitions,
            PromptTemplateEnterpriseConfigDTO enterpriseConfig
    ) {
        return toJson(PromptTemplateExtDTO.builder()
                .variableDefinitions(variableDefinitions == null ? Collections.emptyList() : variableDefinitions)
                .enterpriseConfig(enterpriseConfig)
                .build());
    }

    private PromptTemplateExtDTO emptyExt() {
        return PromptTemplateExtDTO.builder()
                .variableDefinitions(Collections.emptyList())
                .enterpriseConfig(null)
                .build();
    }
}
