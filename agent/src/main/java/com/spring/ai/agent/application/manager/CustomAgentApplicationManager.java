package com.spring.ai.agent.application.manager;

import com.spring.ai.agent.application.assmbler.CustomAgentAssembler;
import com.spring.ai.agent.application.service.custom.DocumentExpertAuditService;
import com.spring.ai.agent.application.service.custom.DocumentExpertEnhancementService;
import com.spring.ai.agent.application.service.custom.DocumentExpertFusionService;
import com.spring.ai.agent.application.service.custom.DocumentExpertGenerationService;
import com.spring.ai.agent.application.service.custom.DocumentExpertLlmInvokeService;
import com.spring.ai.agent.application.service.custom.DocumentExpertModelSupportService;
import com.spring.ai.agent.application.service.custom.DocumentExpertRoutingService;
import com.spring.ai.agent.domain.dto.AccessDecisionDTO;
import com.spring.ai.agent.domain.dto.AuditResultDTO;
import com.spring.ai.agent.domain.dto.EnhancementResultDTO;
import com.spring.ai.agent.domain.dto.FusionResultDTO;
import com.spring.ai.agent.domain.dto.GenerationResultDTO;
import com.spring.ai.agent.domain.dto.StageModelSelectionDTO;
import com.spring.ai.agent.domain.request.DocumentExpertChatRequest;
import com.spring.ai.agent.domain.response.DocumentExpertChatResponse;
import com.spring.ai.agent.domain.response.DocumentExpertChatResponse.StageResult;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义 Agent 应用层编排器。
 * 负责组织无需手工创建的场景化 Agent 调用链。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class CustomAgentApplicationManager {

    private static final String DOCUMENT_AGENT_NAME = "文档专家 Agent";

    @Resource
    private DocumentExpertModelSupportService documentExpertModelSupportService;

    @Resource
    private DocumentExpertRoutingService documentExpertRoutingService;

    @Resource
    private DocumentExpertEnhancementService documentExpertEnhancementService;

    @Resource
    private DocumentExpertGenerationService documentExpertGenerationService;

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    @Resource
    private DocumentExpertAuditService documentExpertAuditService;

    @Resource
    private DocumentExpertFusionService documentExpertFusionService;

    @Resource(name = CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;

    /**
     * 查询文档专家 Agent 可选模型。
     *
     * @return 已启用模型列表
     */
    public List<ModelOptionResponse> listDocumentExpertModels() {
        return documentExpertModelSupportService.listDocumentExpertModels();
    }

    /**
     * 执行文档专家链路。
     *
     * @param request 文档专家请求
     * @return 文档专家响应
     */
    public DocumentExpertChatResponse chatWithDocumentExpert(DocumentExpertChatRequest request) {
        validateDocumentRequest(request);
        StageModelSelectionDTO stageModels = documentExpertModelSupportService.resolveStageModels(request);
        String defaultModelCode = stageModels.getDefaultModelCode();
        String userPrompt = request.getUserPrompt().trim();

        ChatClient routeChatClient = documentExpertModelSupportService.createChatClient(stageModels.getRouteModelCode());
        ChatClient enhancementChatClient = documentExpertModelSupportService.createChatClient(stageModels.getEnhancementModelCode());
        ChatClient auditChatClient = documentExpertModelSupportService.createChatClient(stageModels.getAuditModelCode());
        ChatClient fusionChatClient = documentExpertModelSupportService.createChatClient(stageModels.getFusionModelCode());

        AccessDecisionDTO accessDecision = documentExpertLlmInvokeService.executeWithStage(
                "路由与准入校验",
                stageModels.getRouteModelCode(),
                () -> documentExpertRoutingService.route(routeChatClient, userPrompt)
        );
        StageResult routeStage = CustomAgentAssembler.buildStageResult(
                "路由与准入校验",
                Boolean.TRUE.equals(accessDecision.getAllowed()) ? "PASSED" : "REJECTED",
                stageModels.getRouteModelCode(),
                accessDecision.getReason(),
                accessDecision.getNormalizedIntent(),
                accessDecision.getIssues()
        );

        if (!Boolean.TRUE.equals(accessDecision.getAllowed())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST,
                    StringUtils.hasText(accessDecision.getReason()) ? accessDecision.getReason()
                            : DOCUMENT_AGENT_NAME + " 仅受理文档相关需求");
        }

        EnhancementResultDTO enhancementResult = documentExpertLlmInvokeService.executeWithStage(
                "提示词增强",
                stageModels.getEnhancementModelCode(),
                () -> documentExpertEnhancementService.enhance(
                        enhancementChatClient,
                        userPrompt,
                        request.getAutoFillMissingInfo() == null || Boolean.TRUE.equals(request.getAutoFillMissingInfo())
                )
        );
        StageResult enhancementStage = CustomAgentAssembler.buildStageResult(
                "提示词增强",
                Boolean.TRUE.equals(enhancementResult.getNeedClarification()) ? "NEED_CLARIFICATION" : "PASSED",
                stageModels.getEnhancementModelCode(),
                enhancementResult.getSummary(),
                enhancementResult.getStructuredInstruction(),
                enhancementResult.getMissingItems()
        );

        if (Boolean.TRUE.equals(enhancementResult.getNeedClarification())) {
            return DocumentExpertChatResponse.builder()
                    .modelCode(defaultModelCode)
                    .userPrompt(userPrompt)
                    .clarificationNeeded(true)
                    .clarificationQuestion(enhancementResult.getClarificationQuestion())
                    .routeStage(routeStage)
                    .enhancementStage(enhancementStage)
                    .warnings(enhancementResult.getMissingItems())
                    .build();
        }

        CompletableFuture<GenerationResultDTO> documentAFuture = CompletableFuture.supplyAsync(
                () -> documentExpertLlmInvokeService.executeWithStage(
                        "双文档生成-A",
                        stageModels.getGenerationAModelCode(),
                        () -> documentExpertGenerationService.generateStructured(
                                stageModels.getGenerationAModelCode(),
                                enhancementResult
                        )
                ),
                commonAsyncExecutor
        );
        CompletableFuture<GenerationResultDTO> documentBFuture = CompletableFuture.supplyAsync(
                () -> documentExpertLlmInvokeService.executeWithStage(
                        "双文档生成-B",
                        stageModels.getGenerationBModelCode(),
                        () -> documentExpertGenerationService.generateReadable(
                                stageModels.getGenerationBModelCode(),
                                enhancementResult
                        )
                ),
                commonAsyncExecutor
        );
        CompletableFuture.allOf(documentAFuture, documentBFuture).join();

        GenerationResultDTO documentA = documentAFuture.join();
        GenerationResultDTO documentB = documentBFuture.join();

        StageResult generationStageA = CustomAgentAssembler.buildStageResult(
                "双文档生成-A",
                "COMPLETED",
                stageModels.getGenerationAModelCode(),
                documentA.getSummary(),
                documentA.getDocument(),
                List.of()
        );
        StageResult generationStageB = CustomAgentAssembler.buildStageResult(
                "双文档生成-B",
                "COMPLETED",
                stageModels.getGenerationBModelCode(),
                documentB.getSummary(),
                documentB.getDocument(),
                List.of()
        );

        AuditResultDTO auditResult = documentExpertLlmInvokeService.executeWithStage(
                "文档审核",
                stageModels.getAuditModelCode(),
                () -> documentExpertAuditService.audit(
                        auditChatClient,
                        userPrompt,
                        enhancementResult,
                        documentA,
                        documentB
                )
        );
        StageResult auditStage = CustomAgentAssembler.buildStageResult(
                "文档审核",
                Boolean.TRUE.equals(auditResult.getHasSevereIssue()) ? "WARNING" : "COMPLETED",
                stageModels.getAuditModelCode(),
                auditResult.getSummary(),
                documentExpertAuditService.buildAuditStageContent(auditResult),
                auditResult.getIssues()
        );

        FusionResultDTO fusionResult = documentExpertLlmInvokeService.executeWithStage(
                "融合汇总",
                stageModels.getFusionModelCode(),
                () -> documentExpertFusionService.fuse(
                        fusionChatClient,
                        userPrompt,
                        enhancementResult,
                        auditResult
                )
        );
        StageResult fusionStage = CustomAgentAssembler.buildStageResult(
                "融合汇总",
                "COMPLETED",
                stageModels.getFusionModelCode(),
                fusionResult.getSummary(),
                fusionResult.getFinalDocument(),
                List.of()
        );

        return DocumentExpertChatResponse.builder()
                .modelCode(defaultModelCode)
                .userPrompt(userPrompt)
                .clarificationNeeded(false)
                .routeStage(routeStage)
                .enhancementStage(enhancementStage)
                .generationStageA(generationStageA)
                .generationStageB(generationStageB)
                .auditStage(auditStage)
                .fusionStage(fusionStage)
                .finalDocument(fusionResult.getFinalDocument())
                .warnings(auditResult.getIssues())
                .build();
    }

    /**
     * 校验文档专家入参。
     */
    private void validateDocumentRequest(DocumentExpertChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请求不能为空");
        }
        if (!StringUtils.hasText(request.getModelCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择默认模型");
        }
        if (!StringUtils.hasText(request.getUserPrompt())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请输入文档需求");
        }
    }

    /**
     * 统一构建阶段结果。
     */
}
