package com.spring.ai.agent.controller;

import com.spring.ai.agent.application.service.agentscope.AgentScopeReactAgentService;
import com.spring.ai.agent.domain.request.AgentScopeReactChatRequest;
import com.spring.ai.agent.domain.response.AgentScopeReactChatResponse;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AgentScope ReAct Agent 控制器。
 * 暴露基于 AgentScope 的推理-行动范式调用入口。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@RestController
@RequestMapping("/agents/agentscope/react")
public class AgentScopeReactController {

    @Resource
    private AgentScopeReactAgentService agentScopeReactAgentService;

    /**
     * 执行 AgentScope ReAct 对话。
     *
     * @param request 对话请求
     * @return 对话结果
     */
    @PostMapping("/chat")
    public ApiResponse<AgentScopeReactChatResponse> chat(@RequestBody AgentScopeReactChatRequest request) {
        return ApiResponse.success(agentScopeReactAgentService.chat(request));
    }
}
