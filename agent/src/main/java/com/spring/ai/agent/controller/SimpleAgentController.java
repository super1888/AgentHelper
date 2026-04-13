package com.spring.ai.agent.controller;

import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.service.SimpleAgentService;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单 Agent 创建入口。
 * 前端可以先通过该接口模拟“勾选参数后创建 Agent”的场景。
 */
@RestController
@RequestMapping("/api/agents/simple")
public class SimpleAgentController {

    @Resource
    private SimpleAgentService simpleAgentService;

    @PostMapping
    public ApiResponse<SimpleAgentCreateResponse> createAgent(@RequestBody SimpleAgentCreateRequest request) {
        return ApiResponse.success(simpleAgentService.createAgent(request));
    }
}
