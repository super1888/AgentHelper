package com.spring.ai.prompt.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.providerInterface.UserProvider;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.PromptTemplateRecordService;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptTemplateSupportManager {

    private static final TypeReference<List<PromptTemplateVariableDTO>> VARIABLE_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private UserProvider userProvider;

    @Resource
    private SyUserService syUserService;

    @Resource
    private SyTenantService syTenantService;

    @Resource
    private PromptTemplateRecordService promptTemplateRecordService;

    @Resource
    private ObjectMapper objectMapper;

    public Long getCurrentUserId() {
        Long userId = userProvider.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "current user not found");
        }
        return userId;
    }

    public String getCurrentUserName() {
        return userProvider.getCurrentUserName();
    }

    public Long getCurrentTenantId() {
        SyUser user = syUserService.getDetailById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "current user not found");
        }
        if (user.getTenantId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "current tenant not found");
        }
        SyTenant tenant = syTenantService.getDetailById(user.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "current tenant not found");
        }
        return tenant.getId();
    }

    public PromptTemplateRecord requirePromptTemplate(Long promptTemplateId) {
        PromptTemplateRecord record = promptTemplateRecordService.getById(promptTemplateId);
        if (record == null || !getCurrentTenantId().equals(record.getTenantId())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "prompt template not found: " + promptTemplateId);
        }
        return record;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "json serialize failed", e);
        }
    }

    public List<PromptTemplateVariableDTO> parseVariableDefinitions(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, VARIABLE_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "json parse failed", e);
        }
    }
}
