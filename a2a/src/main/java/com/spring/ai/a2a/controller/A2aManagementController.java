package com.spring.ai.a2a.controller;

import com.spring.ai.a2a.application.manager.A2aApplicationManager;
import com.spring.ai.a2a.domain.request.A2aAgentCardSaveRequest;
import com.spring.ai.a2a.domain.request.A2aDispatchRequest;
import com.spring.ai.a2a.domain.request.A2aRouteSaveRequest;
import com.spring.ai.a2a.domain.response.A2aAgentCardResponse;
import com.spring.ai.a2a.domain.response.A2aLogResponse;
import com.spring.ai.a2a.domain.response.A2aRouteResponse;
import com.spring.ai.a2a.domain.response.A2aStatisticsResponse;
import com.spring.ai.a2a.domain.response.A2aTaskResponse;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/a2a")
public class A2aManagementController {

    @Resource
    private A2aApplicationManager a2aApplicationManager;

    @GetMapping("/agents")
    public ApiResponse<List<A2aAgentCardResponse>> listAgentCards() {
        return ApiResponse.success(a2aApplicationManager.listAgentCards());
    }

    @GetMapping("/agents/deleted")
    public ApiResponse<List<A2aAgentCardResponse>> listDeletedAgentCards() {
        return ApiResponse.success(a2aApplicationManager.listDeletedAgentCards());
    }

    @PostMapping("/agents")
    public ApiResponse<A2aAgentCardResponse> saveAgentCard(@RequestBody A2aAgentCardSaveRequest request) {
        return ApiResponse.success(a2aApplicationManager.saveAgentCard(request));
    }

    @PostMapping("/agents/{id}/publish")
    public ApiResponse<A2aAgentCardResponse> publishAgentCard(@PathVariable Long id) {
        return ApiResponse.success(a2aApplicationManager.publishAgentCard(id));
    }

    @DeleteMapping("/agents/{id}")
    public ApiResponse<Void> deleteAgentCard(@PathVariable Long id) {
        a2aApplicationManager.deleteAgentCard(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/agents/{id}/restore")
    public ApiResponse<A2aAgentCardResponse> restoreAgentCard(@PathVariable Long id) {
        return ApiResponse.success(a2aApplicationManager.restoreAgentCard(id));
    }

    @GetMapping("/routes")
    public ApiResponse<List<A2aRouteResponse>> listRoutes() {
        return ApiResponse.success(a2aApplicationManager.listRoutes());
    }

    @PostMapping("/routes")
    public ApiResponse<A2aRouteResponse> saveRoute(@RequestBody A2aRouteSaveRequest request) {
        return ApiResponse.success(a2aApplicationManager.saveRoute(request));
    }

    @PostMapping("/dispatch")
    public ApiResponse<A2aTaskResponse> dispatch(@RequestBody A2aDispatchRequest request) {
        return ApiResponse.success(a2aApplicationManager.dispatch(request));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<A2aTaskResponse>> listTasks() {
        return ApiResponse.success(a2aApplicationManager.listTasks());
    }

    @GetMapping("/logs")
    public ApiResponse<List<A2aLogResponse>> listLogs(@RequestParam(required = false) String taskCode) {
        return ApiResponse.success(a2aApplicationManager.listLogs(taskCode));
    }

    @PostMapping("/statistics")
    public ApiResponse<A2aStatisticsResponse> statistics() {
        return ApiResponse.success(a2aApplicationManager.statistics());
    }
}
