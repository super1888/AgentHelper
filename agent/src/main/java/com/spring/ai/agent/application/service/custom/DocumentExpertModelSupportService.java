package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.StageModelSelectionDTO;
import com.spring.ai.agent.domain.request.DocumentExpertChatRequest;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文档专家模型支撑服务。
 * 负责模型列表、阶段模型选择、客户端创建与 JSON 解析。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertModelSupportService {

    @Resource
    private CoreApplicationManager coreApplicationManager;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    /**
     * 查询文档专家可选模型。
     */
    public List<ModelOptionResponse> listDocumentExpertModels() {
        return coreApplicationManager.listEnabledModelOptions().stream()
                .filter(item -> "CHAT".equalsIgnoreCase(item.getModelType()))
                .toList();
    }

    /**
     * 解析各阶段模型配置。
     */
    public StageModelSelectionDTO resolveStageModels(DocumentExpertChatRequest request) {
        String defaultModelCode = normalizeAndValidateModelCode(request.getModelCode(), "默认模型");
        return new StageModelSelectionDTO(
                defaultModelCode,
                normalizeStageModelCode(request.getRouteModelCode(), defaultModelCode, "路由与准入校验阶段模型"),
                normalizeStageModelCode(request.getEnhancementModelCode(), defaultModelCode, "提示词增强阶段模型"),
                normalizeStageModelCode(request.getGenerationAModelCode(), defaultModelCode, "双文档生成A阶段模型"),
                normalizeStageModelCode(request.getGenerationBModelCode(), defaultModelCode, "双文档生成B阶段模型"),
                normalizeStageModelCode(request.getAuditModelCode(), defaultModelCode, "文档审核阶段模型"),
                normalizeStageModelCode(request.getFusionModelCode(), defaultModelCode, "融合汇总阶段模型")
        );
    }

    /**
     * 创建对话客户端。
     */
    public ChatClient createChatClient(String modelCode) {
        ChatModel chatModel = coreApplicationManager.createChatModel(modelCode);
        return ChatClient.create(chatModel);
    }

    /**
     * 安全解析模型返回 JSON。
     */
    public <T> T parseJsonOrNull(String rawContent, Class<T> clazz) {
        String json = com.spring.ai.common.utils.CommonTextUtils.extractJsonBody(rawContent);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return commonJsonUtils.parseObject(json, clazz);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private String normalizeStageModelCode(String candidate, String fallbackModelCode, String stageName) {
        String modelCode = StringUtils.hasText(candidate) ? candidate.trim() : fallbackModelCode;
        return normalizeAndValidateModelCode(modelCode, stageName);
    }

    private String normalizeAndValidateModelCode(String modelCode, String stageName) {
        if (!StringUtils.hasText(modelCode)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, stageName + "不能为空");
        }
        String normalized = modelCode.trim();
        coreApplicationManager.getEnabledModelOption(normalized);
        return normalized;
    }
}
