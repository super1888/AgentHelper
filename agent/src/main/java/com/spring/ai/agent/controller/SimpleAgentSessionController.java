package com.spring.ai.agent.controller;

import com.spring.ai.agent.domain.request.SimpleAgentRecoverRequest;
import com.spring.ai.agent.domain.response.SimpleAgentRecoverResponse;
import com.spring.ai.agent.service.SimpleAgentChatService;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话任务恢复入口。
 */
@RestController
@RequestMapping("/agents/simple/sessions")
class SimpleAgentSessionController {

    @Resource
    private SimpleAgentChatService simpleAgentChatService;

    /**
     * 恢复失败任务。
     */
    @PostMapping("/{sessionId}/recover")
    public ApiResponse<SimpleAgentRecoverResponse> recoverTask(
            @PathVariable("sessionId") String sessionId,
            @RequestBody(required = false) SimpleAgentRecoverRequest request
    ) {
        return ApiResponse.success(simpleAgentChatService.recoverTask(sessionId, request));
    }
}
