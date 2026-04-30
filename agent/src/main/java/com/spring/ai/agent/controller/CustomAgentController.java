package com.spring.ai.agent.controller;

import com.spring.ai.agent.application.manager.CustomAgentApplicationManager;
import com.spring.ai.agent.domain.request.DocumentExpertChatRequest;
import com.spring.ai.agent.domain.response.DocumentExpertChatResponse;
import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义 Agent 控制器。
 * 负责暴露无需用户手动创建即可直接使用的场景化 Agent 能力。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@RestController
@RequestMapping("/agents/custom")
public class CustomAgentController {

    @Resource
    private CustomAgentApplicationManager customAgentApplicationManager;

    /**
     * 查询文档专家 Agent 可选模型列表。
     *
     * @return 已启用模型列表
     */
    @GetMapping("/document-expert/models")
    public ApiResponse<List<ModelOptionResponse>> listDocumentExpertModels() {
        return ApiResponse.success(customAgentApplicationManager.listDocumentExpertModels());
    }

    /**
     * 执行文档专家 Agent。
     *
     * @param request 文档专家请求
     * @return 文档生成结果
     */
    @PostMapping("/document-expert/chat")
    public ApiResponse<DocumentExpertChatResponse> chatWithDocumentExpert(
            @RequestBody DocumentExpertChatRequest request
    ) {
        return ApiResponse.success(customAgentApplicationManager.chatWithDocumentExpert(request));
    }
}
