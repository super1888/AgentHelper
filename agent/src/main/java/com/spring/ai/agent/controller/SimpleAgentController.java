package com.spring.ai.agent.controller;

import com.spring.ai.agent.application.manager.SimpleAgentApplicationManager;
import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentReconnectRequest;
import com.spring.ai.agent.domain.request.SimpleAgentSessionCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentUpdateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.domain.response.SimpleAgentDetailResponse;
import com.spring.ai.agent.domain.response.SimpleAgentReconnectResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSessionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSummaryResponse;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private SimpleAgentApplicationManager simpleAgentApplicationManager;

    /**
     * 查询当前用户的 Agent 列表。
     *
     * @return Agent 概览列表
     */
    @GetMapping
    public ApiResponse<List<SimpleAgentSummaryResponse>> listAgents() {
        return ApiResponse.success(simpleAgentApplicationManager.listAgents());
    }

    /**
     * 创建 Agent。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping
    public ApiResponse<SimpleAgentCreateResponse> createAgent(@RequestBody SimpleAgentCreateRequest request) {
        return ApiResponse.success(simpleAgentApplicationManager.createAgent(request));
    }

    /**
     * 更新 Agent，并生成新的版本快照。
     *
     * @param agentId Agent 编码
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/{agentId}")
    public ApiResponse<SimpleAgentCreateResponse> updateAgent(
            @PathVariable("agentId") String agentId,
            @RequestBody SimpleAgentUpdateRequest request
    ) {
        return ApiResponse.success(simpleAgentApplicationManager.updateAgent(agentId, request));
    }

    /**
     * 发布指定版本。
     *
     * @param agentId Agent 编码
     * @param versionNo 版本号
     * @return 空响应
     */
    @PostMapping("/{agentId}/publish")
    public ApiResponse<Void> publishAgent(
            @PathVariable("agentId") String agentId,
            @RequestParam(value = "versionNo", required = false) Integer versionNo
    ) {
        simpleAgentApplicationManager.publishAgent(agentId, versionNo);
        return ApiResponse.success(null);
    }

    /**
     * 禁用 Agent。
     *
     * @param agentId Agent 编码
     * @return 空响应
     */
    @PostMapping("/{agentId}/disable")
    public ApiResponse<Void> disableAgent(@PathVariable("agentId") String agentId) {
        simpleAgentApplicationManager.disableAgent(agentId);
        return ApiResponse.success(null);
    }

    /**
     * 删除 Agent。
     *
     * @param agentId Agent 编码
     * @return 空响应
     */
    @DeleteMapping("/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable("agentId") String agentId) {
        simpleAgentApplicationManager.deleteAgent(agentId);
        return ApiResponse.success(null);
    }

    /**
     * 查询 Agent 详情。
     *
     * @param agentId Agent 编码
     * @return Agent 详情
     */
    @GetMapping("/{agentId}")
    public ApiResponse<SimpleAgentDetailResponse> getAgentDetail(@PathVariable("agentId") String agentId) {
        return ApiResponse.success(simpleAgentApplicationManager.getAgentDetail(agentId));
    }

    /**
     * 创建会话。
     *
     * @param agentId Agent 编码
     * @param request 会话创建请求
     * @return 会话信息
     */
    @PostMapping("/{agentId}/sessions")
    public ApiResponse<SimpleAgentSessionResponse> createSession(
            @PathVariable("agentId") String agentId,
            @RequestBody(required = false) SimpleAgentSessionCreateRequest request
    ) {
        return ApiResponse.success(simpleAgentApplicationManager.createSession(agentId, request));
    }

    /**
     * 会话重连并补发缺失事件。
     *
     * @param sessionId 会话编码
     * @param request 重连请求
     * @return 重连结果
     */
    @PostMapping("/sessions/{sessionId}/reconnect")
    public ApiResponse<SimpleAgentReconnectResponse> reconnectSession(
            @PathVariable("sessionId") String sessionId,
            @RequestBody(required = false) SimpleAgentReconnectRequest request
    ) {
        return ApiResponse.success(simpleAgentApplicationManager.reconnectSession(sessionId, request));
    }

    /**
     * 关闭会话。
     *
     * @param sessionId 会话编码
     * @return 空响应
     */
    @PostMapping("/sessions/{sessionId}/close")
    public ApiResponse<Void> closeSession(@PathVariable("sessionId") String sessionId) {
        simpleAgentApplicationManager.closeSession(sessionId);
        return ApiResponse.success(null);
    }
}
