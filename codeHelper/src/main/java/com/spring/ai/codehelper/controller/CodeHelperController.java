package com.spring.ai.codehelper.controller;

import com.spring.ai.codehelper.application.manager.CodeHelperApplicationManager;
import com.spring.ai.codehelper.application.subagent.CodeHelperSubAgentManager;
import com.spring.ai.codehelper.domain.dto.CodeHelperSubAgentDefinitionDTO;
import com.spring.ai.codehelper.domain.request.CodeHelperCompactRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperMessageRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperPermissionCheckRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperSessionCreateRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperSubAgentRunRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperToolExecuteRequest;
import com.spring.ai.codehelper.domain.response.CodeHelperContextResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperPermissionDecisionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperSessionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperSubAgentRunResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperToolExecutionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperToolLogResponse;
import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.tools.codehelper.CodeHelperToolDescriptor;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * codeHelper 对外控制器。
 */
@RestController
@RequestMapping("/code-helper")
public class CodeHelperController {

    @Resource
    private CodeHelperApplicationManager codeHelperApplicationManager;

    @Resource
    private CodeHelperSubAgentManager codeHelperSubAgentManager;

    @PostMapping("/sessions")
    public ApiResponse<CodeHelperSessionResponse> createSession(@RequestBody CodeHelperSessionCreateRequest request) {
        return ApiResponse.success(codeHelperApplicationManager.createSession(request));
    }

    @PostMapping("/sessions/send")
    public ApiResponse<CodeHelperSessionResponse> sendMessage(@RequestParam String sessionId,
                                                              @RequestBody CodeHelperMessageRequest request) {
        return ApiResponse.success(codeHelperApplicationManager.sendMessage(sessionId, request));
    }

    @PostMapping("/tool/execute")
    public ApiResponse<CodeHelperToolExecutionResponse> executeTool(@RequestBody CodeHelperToolExecuteRequest request) {
        return ApiResponse.success(codeHelperApplicationManager.executeTool(request));
    }

    @PostMapping("/permission/check")
    public ApiResponse<CodeHelperPermissionDecisionResponse> checkPermission(@RequestBody CodeHelperPermissionCheckRequest request) {
        return ApiResponse.success(codeHelperApplicationManager.checkPermission(request));
    }

    @GetMapping("/context")
    public ApiResponse<CodeHelperContextResponse> buildContext(@RequestParam String sessionId) {
        return ApiResponse.success(codeHelperApplicationManager.buildContext(sessionId));
    }

    @PostMapping("/context/compact")
    public ApiResponse<CodeHelperContextResponse> compactContext(@RequestParam String sessionId,
                                                                 @RequestBody(required = false) CodeHelperCompactRequest request) {
        return ApiResponse.success(codeHelperApplicationManager.compactContext(sessionId, request));
    }

    @GetMapping("/prompt")
    public ApiResponse<String> buildSystemPrompt(@RequestParam String sessionId) {
        return ApiResponse.success(codeHelperApplicationManager.buildSystemPrompt(sessionId));
    }

    @GetMapping("/tools")
    public ApiResponse<List<CodeHelperToolDescriptor>> listTools() {
        return ApiResponse.success(codeHelperApplicationManager.listTools());
    }

    @GetMapping("/sub-agents")
    public ApiResponse<List<CodeHelperSubAgentDefinitionDTO>> listSubAgents() {
        return ApiResponse.success(codeHelperSubAgentManager.listSubAgents());
    }

    @PostMapping("/sub-agents/run")
    public ApiResponse<CodeHelperSubAgentRunResponse> runSubAgent(@RequestParam String sessionId,
                                                                  @RequestBody CodeHelperSubAgentRunRequest request) {
        return ApiResponse.success(codeHelperSubAgentManager.runSubAgent(sessionId, request));
    }

    @GetMapping("/models/options")
    public ApiResponse<List<ModelOptionResponse>> listModelOptions() {
        return ApiResponse.success(codeHelperApplicationManager.listModelOptions());
    }

    @GetMapping("/tool/logs")
    public ApiResponse<List<CodeHelperToolLogResponse>> listToolLogs(@RequestParam(required = false) String sessionId) {
        return ApiResponse.success(codeHelperApplicationManager.listToolLogs(sessionId));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<CodeHelperSessionResponse>> listSessions() {
        return ApiResponse.success(codeHelperApplicationManager.listSessions());
    }
}
