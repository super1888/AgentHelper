package com.spring.ai.agent.controller;

import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentReconnectRequest;
import com.spring.ai.agent.domain.request.SimpleAgentSessionCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentUpdateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.domain.response.SimpleAgentDetailResponse;
import com.spring.ai.agent.domain.response.SimpleAgentReconnectResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSessionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSummaryResponse;
import com.spring.ai.agent.service.SimpleAgentService;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple Agent 管理入口。
 */
@RestController
@RequestMapping("/agents/simple")
public class SimpleAgentController {

    @Resource
    private SimpleAgentService simpleAgentService;

    /**
     * 查询当前用户的 Agent 列表。
     */
    @GetMapping
    public ApiResponse<List<SimpleAgentSummaryResponse>> listAgents() {
        return ApiResponse.success(simpleAgentService.listAgents());
    }

    /**
     * 创建 Agent。
     */
    @PostMapping
    public ApiResponse<SimpleAgentCreateResponse> createAgent(@RequestBody SimpleAgentCreateRequest request) {
        return ApiResponse.success(simpleAgentService.createAgent(request));
    }

    /**
     * 更新 Agent，并生成新版本。
     */
    @PatchMapping("/{agentId}")
    public ApiResponse<SimpleAgentCreateResponse> updateAgent(
            @PathVariable("agentId") String agentId,
            @RequestBody SimpleAgentUpdateRequest request
    ) {
        return ApiResponse.success(simpleAgentService.updateAgent(agentId, request));
    }

    /**
     * 发布指定版本。
     */
    @PostMapping("/{agentId}/publish")
    public ApiResponse<Void> publishAgent(
            @PathVariable("agentId") String agentId,
            @RequestParam(value = "versionNo", required = false) Integer versionNo
    ) {
        simpleAgentService.publishAgent(agentId, versionNo);
        return ApiResponse.success(null);
    }

    /**
     * 禁用 Agent。
     */
    @PostMapping("/{agentId}/disable")
    public ApiResponse<Void> disableAgent(@PathVariable("agentId") String agentId) {
        simpleAgentService.disableAgent(agentId);
        return ApiResponse.success(null);
    }

    /**
     * 查询 Agent 详情。
     */
    @GetMapping("/{agentId}")
    public ApiResponse<SimpleAgentDetailResponse> getAgentDetail(@PathVariable("agentId") String agentId) {
        return ApiResponse.success(simpleAgentService.getAgentDetail(agentId));
    }

    /**
     * 创建会话。
     */
    @PostMapping("/{agentId}/sessions")
    public ApiResponse<SimpleAgentSessionResponse> createSession(
            @PathVariable("agentId") String agentId,
            @RequestBody(required = false) SimpleAgentSessionCreateRequest request
    ) {
        return ApiResponse.success(simpleAgentService.createSession(agentId, request));
    }

    /**
     * 会话重连补发。
     */
    @PostMapping("/sessions/{sessionId}/reconnect")
    public ApiResponse<SimpleAgentReconnectResponse> reconnectSession(
            @PathVariable("sessionId") String sessionId,
            @RequestBody(required = false) SimpleAgentReconnectRequest request
    ) {
        return ApiResponse.success(simpleAgentService.reconnectSession(sessionId, request));
    }

    /**
     * 关闭会话。
     */
    @PostMapping("/sessions/{sessionId}/close")
    public ApiResponse<Void> closeSession(@PathVariable("sessionId") String sessionId) {
        simpleAgentService.closeSession(sessionId);
        return ApiResponse.success(null);
    }
}
