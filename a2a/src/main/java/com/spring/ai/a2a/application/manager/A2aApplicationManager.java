package com.spring.ai.a2a.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.a2a.config.A2aManagementConstants;
import com.spring.ai.a2a.domain.request.A2aAgentCardSaveRequest;
import com.spring.ai.a2a.domain.request.A2aDispatchRequest;
import com.spring.ai.a2a.domain.request.A2aRouteSaveRequest;
import com.spring.ai.a2a.domain.response.A2aAgentCardResponse;
import com.spring.ai.a2a.domain.response.A2aLogResponse;
import com.spring.ai.a2a.domain.response.A2aRouteResponse;
import com.spring.ai.a2a.domain.response.A2aStatisticsResponse;
import com.spring.ai.a2a.domain.response.A2aTaskResponse;
import com.spring.ai.a2a.provider.RemoteAgent;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.A2aAgentCardRecord;
import com.spring.ai.common.repository.enitiy.A2aExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.A2aRouteRecord;
import com.spring.ai.common.repository.enitiy.A2aTaskRecord;
import com.spring.ai.common.repository.service.A2aAgentCardRecordService;
import com.spring.ai.common.repository.service.A2aExecutionLogRecordService;
import com.spring.ai.common.repository.service.A2aRouteRecordService;
import com.spring.ai.common.repository.service.A2aTaskRecordService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：A2A 管理应用服务
 * 核心职责：管理 Agent Card、路由策略、跨 Agent 派发任务和审计日志
 */
@Component
public class A2aApplicationManager {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private A2aAgentCardRecordService agentCardRecordService;

    @Resource
    private A2aRouteRecordService routeRecordService;

    @Resource
    private A2aTaskRecordService taskRecordService;

    @Resource
    private A2aExecutionLogRecordService executionLogRecordService;

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RemoteAgent remoteAgent;

    public List<A2aAgentCardResponse> listAgentCards() {
        return agentCardRecordService.listByTenantId(currentTenantId()).stream().map(this::toAgentCardResponse).toList();
    }

    public List<A2aAgentCardResponse> listDeletedAgentCards() {
        return agentCardRecordService.listDeletedByTenantId(currentTenantId()).stream().map(this::toAgentCardResponse).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public A2aAgentCardResponse saveAgentCard(A2aAgentCardSaveRequest request) {
        validateAgentCard(request);
        Long tenantId = currentTenantId();
        A2aAgentCardRecord record = agentCardRecordService.getByAgentCode(tenantId, request.getAgentCode());
        if (record == null) {
            record = new A2aAgentCardRecord();
            record.setAgentCode(trim(request.getAgentCode()));
            record.setTenantId(tenantId);
            record.setDeletedFlag(0);
            record.setPublishStatus(A2aManagementConstants.PUBLISH_DRAFT);
        }
        mergeAgentCard(record, request);
        record.setExt(toJson(Map.of(
                "capabilities", emptyIfNull(request.getCapabilities()),
                "inputModes", emptyIfNull(request.getInputModes()),
                "outputModes", emptyIfNull(request.getOutputModes()),
                "dispatchConfig", Map.of("retryTimes", normalizeRetryTimes(request.getRetryTimes())),
                "authConfig", safeMap(request.getAuthConfig()),
                "metadata", safeMap(request.getMetadata())
        )));
        agentCardRecordService.saveOrUpdate(record);
        return toAgentCardResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public A2aAgentCardResponse publishAgentCard(Long id) {
        A2aAgentCardRecord record = requireAgentCard(id);
        record.setPublishStatus(A2aManagementConstants.PUBLISH_PUBLISHED);
        record.setAgentStatus(A2aManagementConstants.STATUS_ENABLED);
        agentCardRecordService.updateById(record);
        return toAgentCardResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAgentCard(Long id) {
        A2aAgentCardRecord record = requireAgentCard(id);
        record.setDeletedFlag(1);
        record.setPublishStatus(A2aManagementConstants.PUBLISH_OFFLINE);
        agentCardRecordService.updateById(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public A2aAgentCardResponse restoreAgentCard(Long id) {
        A2aAgentCardRecord record = agentCardRecordService.getById(id);
        if (record == null || !Objects.equals(record.getTenantId(), currentTenantId()) || !Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw notFound("A2A Agent Card not found: " + id);
        }
        record.setDeletedFlag(0);
        agentCardRecordService.updateById(record);
        return toAgentCardResponse(record);
    }

    public List<A2aRouteResponse> listRoutes() {
        return routeRecordService.listByTenantId(currentTenantId()).stream().map(this::toRouteResponse).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public A2aRouteResponse saveRoute(A2aRouteSaveRequest request) {
        validateRoute(request);
        Long tenantId = currentTenantId();
        A2aRouteRecord record = routeRecordService.getByRouteCode(tenantId, request.getRouteCode());
        if (record == null) {
            record = new A2aRouteRecord();
            record.setRouteCode(trim(request.getRouteCode()));
            record.setTenantId(tenantId);
        }
        record.setRouteName(trim(request.getRouteName()));
        record.setSourceAgentCode(trimToNull(request.getSourceAgentCode()));
        record.setTargetAgentCode(trim(request.getTargetAgentCode()));
        record.setTaskType(trim(request.getTaskType()));
        record.setRouteStatus(defaultText(request.getRouteStatus(), A2aManagementConstants.STATUS_ENABLED));
        record.setPriorityNo(request.getPriorityNo() == null ? 100 : request.getPriorityNo());
        record.setFailoverEnabled(request.getFailoverEnabled() == null ? 0 : request.getFailoverEnabled());
        record.setFallbackAgentCodes(trimToNull(request.getFallbackAgentCodes()));
        record.setRemark(trimToNull(request.getRemark()));
        routeRecordService.saveOrUpdate(record);
        return toRouteResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public A2aTaskResponse dispatch(A2aDispatchRequest request) {
        if (request == null || !StringUtils.hasText(request.getTaskType())) {
            throw badRequest("taskType is required");
        }
        Long tenantId = currentTenantId();
        long startedAt = System.currentTimeMillis();
        A2aRouteRecord route = routeRecordService.matchRoute(tenantId, request.getSourceAgentCode(), request.getTaskType());
        String primaryAgentCode = StringUtils.hasText(request.getTargetAgentCode())
                ? request.getTargetAgentCode().trim()
                : route == null ? null : route.getTargetAgentCode();
        if (!StringUtils.hasText(primaryAgentCode)) {
            throw badRequest("No target agent matched");
        }

        A2aTaskRecord task = new A2aTaskRecord();
        task.setTaskCode(UUID.randomUUID().toString());
        task.setTaskType(trim(request.getTaskType()));
        task.setSourceAgentCode(trimToNull(request.getSourceAgentCode()));
        task.setTargetAgentCode(primaryAgentCode);
        task.setRouteCode(route == null ? null : route.getRouteCode());
        task.setRequestPayloadJson(toJson(safeMap(request.getPayload())));
        task.setTenantId(tenantId);
        String traceId = UUID.randomUUID().toString();
        try {
            Map<String, Object> response = invokeWithRetryAndFallback(request, tenantId, route, primaryAgentCode, task, traceId);
            task.setTargetAgentCode(String.valueOf(response.get("targetAgentCode")));
            task.setTaskStatus(A2aManagementConstants.TASK_SUCCESS);
            task.setResponsePayloadJson(toJson(response));
            task.setElapsedMs(System.currentTimeMillis() - startedAt);
            taskRecordService.save(task);
        } catch (RuntimeException e) {
            task.setTaskStatus(A2aManagementConstants.TASK_FAILED);
            task.setFailureReason(e.getMessage());
            task.setElapsedMs(System.currentTimeMillis() - startedAt);
            taskRecordService.save(task);
        }
        return toTaskResponse(task);
    }

    public List<A2aTaskResponse> listTasks() {
        return taskRecordService.listByTenantId(currentTenantId()).stream().map(this::toTaskResponse).toList();
    }

    public List<A2aLogResponse> listLogs(String taskCode) {
        Long tenantId = currentTenantId();
        List<A2aExecutionLogRecord> logs = StringUtils.hasText(taskCode)
                ? executionLogRecordService.listByTaskCode(tenantId, taskCode)
                : executionLogRecordService.listByTenantId(tenantId);
        return logs.stream().map(this::toLogResponse).toList();
    }

    public A2aStatisticsResponse statistics() {
        Long tenantId = currentTenantId();
        List<A2aAgentCardRecord> agents = agentCardRecordService.listByTenantId(tenantId);
        List<A2aRouteRecord> routes = routeRecordService.listByTenantId(tenantId);
        List<A2aTaskRecord> tasks = taskRecordService.listByTenantId(tenantId);
        List<A2aExecutionLogRecord> logs = executionLogRecordService.listByTenantId(tenantId);
        return A2aStatisticsResponse.builder()
                .agentCount(agents.size())
                .publishedAgentCount((int) agents.stream().filter(item -> A2aManagementConstants.PUBLISH_PUBLISHED.equals(item.getPublishStatus())).count())
                .routeCount(routes.size())
                .taskCount(tasks.size())
                .successTaskCount((int) tasks.stream().filter(item -> A2aManagementConstants.TASK_SUCCESS.equals(item.getTaskStatus())).count())
                .failedTaskCount((int) tasks.stream().filter(item -> A2aManagementConstants.TASK_FAILED.equals(item.getTaskStatus())).count())
                .logCount(logs.size())
                .build();
    }

    private void mergeAgentCard(A2aAgentCardRecord record, A2aAgentCardSaveRequest request) {
        record.setAgentName(trim(request.getAgentName()));
        record.setDescription(trimToNull(request.getDescription()));
        record.setEndpointUrl(trim(request.getEndpointUrl()));
        record.setProtocolVersion(defaultText(request.getProtocolVersion(), "1.0"));
        record.setTransportType(defaultText(request.getTransportType(), "HTTP"));
        record.setAuthType(defaultText(request.getAuthType(), "NONE"));
        record.setAgentStatus(defaultText(request.getAgentStatus(), A2aManagementConstants.STATUS_ENABLED));
        record.setRiskLevel(defaultText(request.getRiskLevel(), "MEDIUM"));
        record.setTrustLevel(defaultText(request.getTrustLevel(), "INTERNAL"));
        record.setOwnerTeam(trimToNull(request.getOwnerTeam()));
        record.setTimeoutMs(request.getTimeoutMs() == null ? 10000 : request.getTimeoutMs());
        record.setRateLimitQps(request.getRateLimitQps() == null ? 10 : request.getRateLimitQps());
        record.setSuccessRateSlo(request.getSuccessRateSlo() == null ? 99 : request.getSuccessRateSlo());
        record.setRemark(trimToNull(request.getRemark()));
    }

    /**
     * 执行远程调用，支持主目标重试和路由 fallback 自动切换。
     */
    private Map<String, Object> invokeWithRetryAndFallback(A2aDispatchRequest request, Long tenantId, A2aRouteRecord route,
                                                           String primaryAgentCode, A2aTaskRecord task, String traceId) {
        List<String> candidateAgentCodes = buildCandidateAgentCodes(request, route, primaryAgentCode);
        List<String> failureReasons = new ArrayList<>();
        int totalAttempts = 0;
        for (String agentCode : candidateAgentCodes) {
            A2aAgentCardRecord target = agentCardRecordService.getByAgentCode(tenantId, agentCode);
            if (target == null || !A2aManagementConstants.PUBLISH_PUBLISHED.equals(target.getPublishStatus())) {
                totalAttempts++;
                String failureReason = agentCode + ": Target agent is not published";
                failureReasons.add(failureReason);
                saveAttemptLog(task, traceId, agentCode, route, totalAttempts, 0, null, null, 0, 0L, failureReason);
                continue;
            }
            int retryTimes = resolveRetryTimes(target);
            for (int retryIndex = 0; retryIndex <= retryTimes; retryIndex++) {
                totalAttempts++;
                long attemptStart = System.currentTimeMillis();
                Map<String, Object> requestPayload = buildDispatchPayload(request, target, route, totalAttempts, retryIndex);
                try {
                    Map<String, Object> remoteResponse = remoteAgent.invokeRemoteAgent(target.getAgentCode(), requestPayload, resolveTimeoutMs(target));
                    saveAttemptLog(task, traceId, target.getAgentCode(), route, totalAttempts, retryIndex, requestPayload, remoteResponse,
                            1, System.currentTimeMillis() - attemptStart, null);
                    return buildSuccessResponse(target, route, remoteResponse, totalAttempts, retryIndex, failureReasons);
                } catch (RuntimeException e) {
                    String failureReason = agentCode + "#" + (retryIndex + 1) + ": " + e.getMessage();
                    failureReasons.add(failureReason);
                    saveAttemptLog(task, traceId, target.getAgentCode(), route, totalAttempts, retryIndex, requestPayload, null,
                            0, System.currentTimeMillis() - attemptStart, failureReason);
                }
            }
        }
        throw new IllegalStateException("All A2A dispatch attempts failed: " + String.join("; ", failureReasons));
    }

    /**
     * 显式指定目标时不自动兜底；路由模式下按主目标、备用目标顺序尝试。
     */
    private List<String> buildCandidateAgentCodes(A2aDispatchRequest request, A2aRouteRecord route, String primaryAgentCode) {
        Set<String> agentCodes = new LinkedHashSet<>();
        agentCodes.add(primaryAgentCode);
        if (!StringUtils.hasText(request.getTargetAgentCode()) && route != null && Integer.valueOf(1).equals(route.getFailoverEnabled())) {
            splitComma(route.getFallbackAgentCodes()).forEach(agentCodes::add);
        }
        return agentCodes.stream().filter(StringUtils::hasText).map(String::trim).toList();
    }

    /**
     * 组装远程 A2A 请求体，补充治理链路所需的上下文信息。
     */
    private Map<String, Object> buildDispatchPayload(A2aDispatchRequest request, A2aAgentCardRecord target, A2aRouteRecord route, int attemptNo, int retryIndex) {
        Map<String, Object> payload = new LinkedHashMap<>(safeMap(request.getPayload()));
        payload.putIfAbsent("taskType", trim(request.getTaskType()));
        payload.putIfAbsent("targetAgentCode", target.getAgentCode());
        payload.putIfAbsent("targetEndpoint", target.getEndpointUrl());
        payload.putIfAbsent("routeCode", route == null ? null : route.getRouteCode());
        payload.putIfAbsent("tenantId", currentTenantId());
        payload.putIfAbsent("attemptNo", attemptNo);
        payload.putIfAbsent("retryIndex", retryIndex);
        Map<String, Object> authConfig = objectMap(parseMap(target.getExt()).get("authConfig"));
        if (!authConfig.isEmpty()) {
            payload.putIfAbsent("authContext", authConfig);
        }
        if (!payload.containsKey("messages") && StringUtils.hasText(request.getTaskType())
                && request.getTaskType().toLowerCase().contains("chat")
                && payload.containsKey("input")) {
            payload.put("messages", List.of(Map.of("role", "user", "content", String.valueOf(payload.get("input")))));
        }
        return payload;
    }

    private Map<String, Object> buildSuccessResponse(A2aAgentCardRecord target, A2aRouteRecord route, Map<String, Object> remoteResponse,
                                                     int totalAttempts, int retryIndex, List<String> failureReasons) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("targetAgentCode", target.getAgentCode());
        response.put("endpointUrl", target.getEndpointUrl());
        response.put("taskAccepted", true);
        response.put("routeCode", route == null ? null : route.getRouteCode());
        response.put("attemptCount", totalAttempts);
        response.put("retryIndex", retryIndex);
        response.put("fallbackUsed", !failureReasons.isEmpty());
        response.put("previousFailures", failureReasons);
        response.put("remoteResult", remoteResponse);
        return response;
    }

    private Integer resolveTimeoutMs(A2aAgentCardRecord target) {
        Integer timeoutMs = target.getTimeoutMs() == null ? 10000 : target.getTimeoutMs();
        return Math.max(1000, Math.min(timeoutMs, 120000));
    }

    private int resolveRetryTimes(A2aAgentCardRecord target) {
        Map<String, Object> ext = parseMap(target.getExt());
        Map<String, Object> dispatchConfig = objectMap(ext.get("dispatchConfig"));
        Object configuredRetryTimes = dispatchConfig.get("retryTimes");
        if (configuredRetryTimes == null) {
            configuredRetryTimes = objectMap(ext.get("metadata")).get("retryTimes");
        }
        if (configuredRetryTimes instanceof Number number) {
            return Math.max(0, Math.min(number.intValue(), 3));
        }
        if (configuredRetryTimes instanceof String text && StringUtils.hasText(text)) {
            try {
                return Math.max(0, Math.min(Integer.parseInt(text.trim()), 3));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private Integer normalizeRetryTimes(Integer retryTimes) {
        return Math.max(0, Math.min(retryTimes == null ? 1 : retryTimes, 3));
    }

    private List<String> splitComma(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void saveAttemptLog(A2aTaskRecord task, String traceId, String targetAgentCode, A2aRouteRecord route,
                                Integer attemptNo, Integer retryIndex, Map<String, Object> requestPayload,
                                Map<String, Object> responsePayload, Integer successFlag, Long elapsedMs, String failureReason) {
        A2aExecutionLogRecord log = new A2aExecutionLogRecord();
        log.setTaskCode(task.getTaskCode());
        log.setTraceId(traceId);
        log.setSourceAgentCode(task.getSourceAgentCode());
        log.setTargetAgentCode(targetAgentCode);
        log.setRouteCode(route == null ? null : route.getRouteCode());
        log.setEventType(A2aManagementConstants.EVENT_DISPATCH);
        log.setExecuteStatus(Integer.valueOf(1).equals(successFlag) ? A2aManagementConstants.TASK_SUCCESS : A2aManagementConstants.TASK_FAILED);
        log.setAttemptNo(attemptNo);
        log.setRetryIndex(retryIndex);
        log.setSuccessFlag(successFlag);
        log.setElapsedMs(elapsedMs);
        log.setRequestPayloadJson(requestPayload == null ? task.getRequestPayloadJson() : toJson(requestPayload));
        log.setResponsePayloadJson(responsePayload == null ? null : toJson(responsePayload));
        log.setFailureReason(failureReason);
        log.setTenantId(task.getTenantId());
        executionLogRecordService.save(log);
    }

    private A2aAgentCardRecord requireAgentCard(Long id) {
        A2aAgentCardRecord record = agentCardRecordService.getById(id);
        if (record == null || !Objects.equals(record.getTenantId(), currentTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw notFound("A2A Agent Card not found: " + id);
        }
        return record;
    }

    private A2aAgentCardResponse toAgentCardResponse(A2aAgentCardRecord record) {
        Map<String, Object> ext = parseMap(record.getExt());
        return A2aAgentCardResponse.builder()
                .id(record.getId()).agentCode(record.getAgentCode()).agentName(record.getAgentName()).description(record.getDescription())
                .endpointUrl(record.getEndpointUrl()).protocolVersion(record.getProtocolVersion()).transportType(record.getTransportType())
                .authType(record.getAuthType()).agentStatus(record.getAgentStatus()).publishStatus(record.getPublishStatus())
                .riskLevel(record.getRiskLevel()).trustLevel(record.getTrustLevel()).ownerTeam(record.getOwnerTeam())
                .timeoutMs(record.getTimeoutMs()).retryTimes(resolveRetryTimes(record)).rateLimitQps(record.getRateLimitQps()).successRateSlo(record.getSuccessRateSlo())
                .capabilities(stringList(ext.get("capabilities"))).inputModes(stringList(ext.get("inputModes"))).outputModes(stringList(ext.get("outputModes")))
                .authConfig(objectMap(ext.get("authConfig"))).metadata(objectMap(ext.get("metadata"))).remark(record.getRemark())
                .createTime(toEpochMilli(record.getCreateTime())).updateTime(toEpochMilli(record.getUpdateTime())).build();
    }

    private A2aRouteResponse toRouteResponse(A2aRouteRecord record) {
        return A2aRouteResponse.builder()
                .id(record.getId()).routeCode(record.getRouteCode()).routeName(record.getRouteName()).sourceAgentCode(record.getSourceAgentCode())
                .targetAgentCode(record.getTargetAgentCode()).taskType(record.getTaskType()).routeStatus(record.getRouteStatus())
                .priorityNo(record.getPriorityNo()).failoverEnabled(record.getFailoverEnabled()).fallbackAgentCodes(record.getFallbackAgentCodes())
                .remark(record.getRemark()).createTime(toEpochMilli(record.getCreateTime())).build();
    }

    private A2aTaskResponse toTaskResponse(A2aTaskRecord record) {
        return A2aTaskResponse.builder()
                .id(record.getId()).taskCode(record.getTaskCode()).taskType(record.getTaskType()).sourceAgentCode(record.getSourceAgentCode())
                .targetAgentCode(record.getTargetAgentCode()).routeCode(record.getRouteCode()).taskStatus(record.getTaskStatus())
                .requestPayloadJson(record.getRequestPayloadJson()).responsePayloadJson(record.getResponsePayloadJson())
                .failureReason(record.getFailureReason()).elapsedMs(record.getElapsedMs()).createTime(toEpochMilli(record.getCreateTime())).build();
    }

    private A2aLogResponse toLogResponse(A2aExecutionLogRecord record) {
        return A2aLogResponse.builder()
                .id(record.getId()).taskCode(record.getTaskCode()).traceId(record.getTraceId()).sourceAgentCode(record.getSourceAgentCode())
                .targetAgentCode(record.getTargetAgentCode()).routeCode(record.getRouteCode()).eventType(record.getEventType())
                .executeStatus(record.getExecuteStatus()).attemptNo(record.getAttemptNo()).retryIndex(record.getRetryIndex()).successFlag(record.getSuccessFlag())
                .elapsedMs(record.getElapsedMs()).failureReason(record.getFailureReason()).createTime(toEpochMilli(record.getCreateTime())).build();
    }

    private void validateAgentCard(A2aAgentCardSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentCode()) || !StringUtils.hasText(request.getAgentName()) || !StringUtils.hasText(request.getEndpointUrl())) {
            throw badRequest("agentCode, agentName and endpointUrl are required");
        }
    }

    private void validateRoute(A2aRouteSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getRouteCode()) || !StringUtils.hasText(request.getTargetAgentCode()) || !StringUtils.hasText(request.getTaskType())) {
            throw badRequest("routeCode, targetAgentCode and taskType are required");
        }
    }

    private Long currentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON parse failed", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON serialize failed", e);
        }
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(java.util.stream.Collectors.toMap(item -> String.valueOf(item.getKey()), Map.Entry::getValue));
        }
        return Map.of();
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StringUtils::hasText).map(String::trim).toList();
        }
        return List.of();
    }

    private List<String> emptyIfNull(List<String> value) {
        return value == null ? List.of() : value.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
