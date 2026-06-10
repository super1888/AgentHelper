package com.spring.ai.a2a.application.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.spring.ai.common.repository.enitiy.A2aAgentCardRecord;
import com.spring.ai.common.repository.enitiy.A2aExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.A2aRouteRecord;
import com.spring.ai.common.repository.enitiy.A2aTaskRecord;
import com.spring.ai.common.repository.service.A2aAgentCardRecordService;
import com.spring.ai.common.repository.service.A2aExecutionLogRecordService;
import com.spring.ai.common.repository.service.A2aRouteRecordService;
import com.spring.ai.common.repository.service.A2aTaskRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class A2aApplicationManagerTest {

    private static final Long TENANT_ID = 1001L;

    @Mock
    private A2aAgentCardRecordService agentCardRecordService;

    @Mock
    private A2aRouteRecordService routeRecordService;

    @Mock
    private A2aTaskRecordService taskRecordService;

    @Mock
    private A2aExecutionLogRecordService executionLogRecordService;

    @Mock
    private CurrentUserContextSupport currentUserContextSupport;

    @Mock
    private RemoteAgent remoteAgent;

    @Captor
    private ArgumentCaptor<A2aAgentCardRecord> agentCaptor;

    @Captor
    private ArgumentCaptor<A2aRouteRecord> routeCaptor;

    @Captor
    private ArgumentCaptor<A2aTaskRecord> taskCaptor;

    @Captor
    private ArgumentCaptor<A2aExecutionLogRecord> logCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    @InjectMocks
    private A2aApplicationManager manager;

    private CommonJsonUtils commonJsonUtils;

    @BeforeEach
    void setUp() {
        commonJsonUtils = new CommonJsonUtils();
        ReflectionTestUtils.setField(commonJsonUtils, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(manager, "commonJsonUtils", commonJsonUtils);
        lenient().when(currentUserContextSupport.getCurrentTenantIdWithAutoInit()).thenReturn(TENANT_ID);
    }

    @Test
    void saveAgentCardCreatesRecordWithDefaultsAndNormalizedExt() {
        A2aAgentCardSaveRequest request = agentRequest();
        request.setAgentCode(" data-agent ");
        request.setAgentName(" Data Agent ");
        request.setEndpointUrl(" http://127.0.0.1/a2a ");
        request.setRetryTimes(9);
        request.setCapabilities(List.of("chat", "analysis"));
        request.setInputModes(List.of("text"));
        request.setOutputModes(List.of("json"));
        request.setAuthConfig(Map.of("token", "abc"));
        request.setMetadata(Map.of("region", "cn"));

        when(agentCardRecordService.getByAgentCode(TENANT_ID, request.getAgentCode())).thenReturn(null);

        A2aAgentCardResponse response = manager.saveAgentCard(request);

        verify(agentCardRecordService).saveOrUpdate(agentCaptor.capture());
        A2aAgentCardRecord saved = agentCaptor.getValue();
        assertThat(saved.getAgentCode()).isEqualTo("data-agent");
        assertThat(saved.getAgentName()).isEqualTo("Data Agent");
        assertThat(saved.getEndpointUrl()).isEqualTo("http://127.0.0.1/a2a");
        assertThat(saved.getProtocolVersion()).isEqualTo("1.0");
        assertThat(saved.getTransportType()).isEqualTo("HTTP");
        assertThat(saved.getAuthType()).isEqualTo("NONE");
        assertThat(saved.getAgentStatus()).isEqualTo(A2aManagementConstants.STATUS_ENABLED);
        assertThat(saved.getPublishStatus()).isEqualTo(A2aManagementConstants.PUBLISH_DRAFT);
        assertThat(saved.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(saved.getTrustLevel()).isEqualTo("INTERNAL");
        assertThat(saved.getTimeoutMs()).isEqualTo(10000);
        assertThat(saved.getRateLimitQps()).isEqualTo(10);
        assertThat(saved.getSuccessRateSlo()).isEqualTo(99);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(response.getRetryTimes()).isEqualTo(3);
        assertThat(response.getCapabilities()).containsExactly("chat", "analysis");
        assertThat(response.getAuthConfig()).containsEntry("token", "abc");
        assertThat(response.getMetadata()).containsEntry("region", "cn");
    }

    @Test
    void saveAgentCardUpdatesExistingRecordAndKeepsIdentityFields() {
        A2aAgentCardRecord existing = publishedAgent("old-code", "http://old");
        existing.setId(10L);
        existing.setPublishStatus(A2aManagementConstants.PUBLISH_PUBLISHED);
        A2aAgentCardSaveRequest request = agentRequest();
        request.setAgentCode("old-code");
        request.setAgentName("new name");
        request.setEndpointUrl("http://new");
        request.setRetryTimes(-1);
        request.setTimeoutMs(500);
        request.setRateLimitQps(20);
        request.setSuccessRateSlo(95);

        when(agentCardRecordService.getByAgentCode(TENANT_ID, "old-code")).thenReturn(existing);

        A2aAgentCardResponse response = manager.saveAgentCard(request);

        verify(agentCardRecordService).saveOrUpdate(agentCaptor.capture());
        assertThat(agentCaptor.getValue()).isSameAs(existing);
        assertThat(agentCaptor.getValue().getAgentCode()).isEqualTo("old-code");
        assertThat(agentCaptor.getValue().getPublishStatus()).isEqualTo(A2aManagementConstants.PUBLISH_PUBLISHED);
        assertThat(response.getRetryTimes()).isZero();
        assertThat(response.getTimeoutMs()).isEqualTo(500);
    }

    @Test
    void saveAgentCardRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> manager.saveAgentCard(null))
                .hasMessageContaining("agentCode, agentName and endpointUrl are required");

        A2aAgentCardSaveRequest request = agentRequest();
        request.setEndpointUrl(" ");
        assertThatThrownBy(() -> manager.saveAgentCard(request))
                .hasMessageContaining("agentCode, agentName and endpointUrl are required");
    }

    @Test
    void publishDeleteAndRestoreAgentCardChangeLifecycleState() {
        A2aAgentCardRecord active = draftAgent(11L, TENANT_ID, 0);
        when(agentCardRecordService.getById(11L)).thenReturn(active);

        A2aAgentCardResponse published = manager.publishAgentCard(11L);
        assertThat(published.getPublishStatus()).isEqualTo(A2aManagementConstants.PUBLISH_PUBLISHED);
        assertThat(published.getAgentStatus()).isEqualTo(A2aManagementConstants.STATUS_ENABLED);
        verify(agentCardRecordService).updateById(active);

        manager.deleteAgentCard(11L);
        assertThat(active.getDeletedFlag()).isEqualTo(1);
        assertThat(active.getPublishStatus()).isEqualTo(A2aManagementConstants.PUBLISH_OFFLINE);

        A2aAgentCardRecord deleted = draftAgent(12L, TENANT_ID, 1);
        when(agentCardRecordService.getById(12L)).thenReturn(deleted);
        A2aAgentCardResponse restored = manager.restoreAgentCard(12L);
        assertThat(restored.getPublishStatus()).isEqualTo(A2aManagementConstants.PUBLISH_DRAFT);
        assertThat(deleted.getDeletedFlag()).isZero();
    }

    @Test
    void agentCardLifecycleRejectsMissingWrongTenantOrDeletedRecord() {
        when(agentCardRecordService.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> manager.publishAgentCard(1L)).hasMessageContaining("A2A Agent Card not found");

        when(agentCardRecordService.getById(2L)).thenReturn(draftAgent(2L, 999L, 0));
        assertThatThrownBy(() -> manager.deleteAgentCard(2L)).hasMessageContaining("A2A Agent Card not found");

        when(agentCardRecordService.getById(3L)).thenReturn(draftAgent(3L, TENANT_ID, 1));
        assertThatThrownBy(() -> manager.publishAgentCard(3L)).hasMessageContaining("A2A Agent Card not found");

        when(agentCardRecordService.getById(4L)).thenReturn(draftAgent(4L, 999L, 1));
        assertThatThrownBy(() -> manager.restoreAgentCard(4L)).hasMessageContaining("A2A Agent Card not found");
    }

    @Test
    void saveRouteCreatesRecordWithDefaultsAndRejectsInvalidRequest() {
        A2aRouteSaveRequest request = routeRequest();
        request.setRouteCode(" route-1 ");
        request.setRouteName(" Route One ");
        request.setSourceAgentCode(" ");
        request.setTargetAgentCode(" target ");
        request.setTaskType(" chat ");
        request.setFallbackAgentCodes(" f1, f2 ");

        when(routeRecordService.getByRouteCode(TENANT_ID, request.getRouteCode())).thenReturn(null);

        A2aRouteResponse response = manager.saveRoute(request);

        verify(routeRecordService).saveOrUpdate(routeCaptor.capture());
        A2aRouteRecord saved = routeCaptor.getValue();
        assertThat(saved.getRouteCode()).isEqualTo("route-1");
        assertThat(saved.getRouteName()).isEqualTo("Route One");
        assertThat(saved.getSourceAgentCode()).isNull();
        assertThat(saved.getTargetAgentCode()).isEqualTo("target");
        assertThat(saved.getTaskType()).isEqualTo("chat");
        assertThat(saved.getRouteStatus()).isEqualTo(A2aManagementConstants.STATUS_ENABLED);
        assertThat(saved.getPriorityNo()).isEqualTo(100);
        assertThat(saved.getFailoverEnabled()).isZero();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(response.getRouteCode()).isEqualTo("route-1");

        A2aRouteSaveRequest invalid = routeRequest();
        invalid.setTaskType(null);
        assertThatThrownBy(() -> manager.saveRoute(invalid))
                .hasMessageContaining("routeCode, targetAgentCode and taskType are required");
    }

    @Test
    void dispatchDirectTargetBuildsGovernancePayloadAndSavesSuccessTask() {
        A2aAgentCardRecord target = publishedAgent("target-agent", "http://target");
        target.setTimeoutMs(200);
        target.setExt(commonJsonUtils.toJson(Map.of("authConfig", Map.of("apiKey", "k"), "dispatchConfig", Map.of("retryTimes", 0))));
        A2aDispatchRequest request = dispatchRequest();
        request.setTargetAgentCode(" target-agent ");
        request.setTaskType("chat-analysis");
        request.setPayload(Map.of("input", "hello"));

        when(routeRecordService.matchRoute(TENANT_ID, "source-agent", "chat-analysis")).thenReturn(null);
        when(agentCardRecordService.getByAgentCode(TENANT_ID, "target-agent")).thenReturn(target);
        when(remoteAgent.invokeRemoteAgent(eq("target-agent"), payloadCaptor.capture(), eq(1000)))
                .thenReturn(Map.of("answer", "ok"));

        A2aTaskResponse response = manager.dispatch(request);

        verify(taskRecordService).save(taskCaptor.capture());
        verify(executionLogRecordService).save(logCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("taskType", "chat-analysis");
        assertThat(payload).containsEntry("targetAgentCode", "target-agent");
        assertThat(payload).containsEntry("targetEndpoint", "http://target");
        assertThat(payload).containsEntry("tenantId", TENANT_ID);
        assertThat(payload).containsEntry("attemptNo", 1);
        assertThat(payload).containsEntry("retryIndex", 0);
        assertThat(payload.get("authContext")).isEqualTo(Map.of("apiKey", "k"));
        assertThat((List<?>) payload.get("messages")).hasSize(1);
        assertThat(taskCaptor.getValue().getTaskStatus()).isEqualTo(A2aManagementConstants.TASK_SUCCESS);
        assertThat(taskCaptor.getValue().getTargetAgentCode()).isEqualTo("target-agent");
        assertThat(response.getTaskStatus()).isEqualTo(A2aManagementConstants.TASK_SUCCESS);
        assertThat(commonJsonUtils.parseMap(taskCaptor.getValue().getResponsePayloadJson()))
                .containsEntry("taskAccepted", true)
                .containsEntry("attemptCount", 1)
                .containsEntry("fallbackUsed", false);
        assertThat(logCaptor.getValue().getSuccessFlag()).isEqualTo(1);
    }

    @Test
    void dispatchUsesRouteFallbackAfterPrimaryUnpublished() {
        A2aRouteRecord route = route("route-1", "primary-agent", "backup-agent, primary-agent, backup-agent");
        A2aAgentCardRecord backup = publishedAgent("backup-agent", "http://backup");
        backup.setExt(commonJsonUtils.toJson(Map.of("metadata", Map.of("retryTimes", "0"))));
        A2aDispatchRequest request = dispatchRequest();
        request.setTargetAgentCode(null);

        when(routeRecordService.matchRoute(TENANT_ID, "source-agent", "chat-analysis")).thenReturn(route);
        when(agentCardRecordService.getByAgentCode(TENANT_ID, "primary-agent")).thenReturn(null);
        when(agentCardRecordService.getByAgentCode(TENANT_ID, "backup-agent")).thenReturn(backup);
        when(remoteAgent.invokeRemoteAgent(eq("backup-agent"), any(), eq(10000))).thenReturn(Map.of("ok", true));

        A2aTaskResponse response = manager.dispatch(request);

        verify(taskRecordService).save(taskCaptor.capture());
        verify(executionLogRecordService, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        assertThat(response.getTaskStatus()).isEqualTo(A2aManagementConstants.TASK_SUCCESS);
        assertThat(response.getTargetAgentCode()).isEqualTo("backup-agent");
        Map<String, Object> savedResponse = commonJsonUtils.parseMap(taskCaptor.getValue().getResponsePayloadJson());
        assertThat(savedResponse).containsEntry("routeCode", "route-1");
        assertThat(savedResponse).containsEntry("attemptCount", 2);
        assertThat(savedResponse).containsEntry("fallbackUsed", true);
        assertThat((List<?>) savedResponse.get("previousFailures")).hasSize(1);
        assertThat(logCaptor.getAllValues()).extracting(A2aExecutionLogRecord::getTargetAgentCode)
                .containsExactly("primary-agent", "backup-agent");
    }

    @Test
    void dispatchRetriesPublishedTargetAndReturnsFailedTaskWhenAllAttemptsFail() {
        A2aAgentCardRecord target = publishedAgent("target-agent", "http://target");
        target.setTimeoutMs(130000);
        target.setExt(commonJsonUtils.toJson(Map.of("dispatchConfig", Map.of("retryTimes", "1"))));
        A2aDispatchRequest request = dispatchRequest();
        request.setTargetAgentCode("target-agent");

        when(routeRecordService.matchRoute(TENANT_ID, "source-agent", "chat-analysis")).thenReturn(null);
        when(agentCardRecordService.getByAgentCode(TENANT_ID, "target-agent")).thenReturn(target);
        when(remoteAgent.invokeRemoteAgent(eq("target-agent"), any(), eq(120000)))
                .thenThrow(new IllegalStateException("first failed"))
                .thenThrow(new IllegalStateException("second failed"));

        A2aTaskResponse response = manager.dispatch(request);

        verify(taskRecordService).save(taskCaptor.capture());
        verify(executionLogRecordService, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        assertThat(response.getTaskStatus()).isEqualTo(A2aManagementConstants.TASK_FAILED);
        assertThat(response.getFailureReason())
                .contains("All A2A dispatch attempts failed")
                .contains("target-agent#1")
                .contains("target-agent#2");
        assertThat(taskCaptor.getValue().getResponsePayloadJson()).isNull();
        assertThat(logCaptor.getAllValues()).extracting(A2aExecutionLogRecord::getRetryIndex).containsExactly(0, 1);
    }

    @Test
    void dispatchRejectsMissingTaskTypeOrTarget() {
        assertThatThrownBy(() -> manager.dispatch(null)).hasMessageContaining("taskType is required");

        A2aDispatchRequest request = dispatchRequest();
        request.setTaskType(" ");
        assertThatThrownBy(() -> manager.dispatch(request)).hasMessageContaining("taskType is required");

        request.setTaskType("chat-analysis");
        request.setTargetAgentCode(null);
        when(routeRecordService.matchRoute(TENANT_ID, "source-agent", "chat-analysis")).thenReturn(null);
        assertThatThrownBy(() -> manager.dispatch(request)).hasMessageContaining("No target agent matched");
        verify(taskRecordService, never()).save(any());
    }

    @Test
    void listQueriesAndStatisticsMapRecordsForCurrentTenant() {
        A2aAgentCardRecord draft = draftAgent(1L, TENANT_ID, 0);
        A2aAgentCardRecord published = publishedAgent("published", "http://p");
        published.setId(2L);
        A2aRouteRecord route = route("route", "published", null);
        A2aTaskRecord successTask = task("SUCCESS_CODE", A2aManagementConstants.TASK_SUCCESS);
        A2aTaskRecord failedTask = task("FAILED_CODE", A2aManagementConstants.TASK_FAILED);
        A2aExecutionLogRecord log = log("SUCCESS_CODE");

        when(agentCardRecordService.listByTenantId(TENANT_ID)).thenReturn(List.of(draft, published));
        when(agentCardRecordService.listDeletedByTenantId(TENANT_ID)).thenReturn(List.of(draft));
        when(routeRecordService.listByTenantId(TENANT_ID)).thenReturn(List.of(route));
        when(taskRecordService.listByTenantId(TENANT_ID)).thenReturn(List.of(successTask, failedTask));
        when(executionLogRecordService.listByTenantId(TENANT_ID)).thenReturn(List.of(log));
        when(executionLogRecordService.listByTaskCode(TENANT_ID, "SUCCESS_CODE")).thenReturn(List.of(log));

        assertThat(manager.listAgentCards()).extracting(A2aAgentCardResponse::getAgentCode).containsExactly("agent-1", "published");
        assertThat(manager.listDeletedAgentCards()).hasSize(1);
        assertThat(manager.listRoutes()).extracting(A2aRouteResponse::getRouteCode).containsExactly("route");
        assertThat(manager.listTasks()).extracting(A2aTaskResponse::getTaskCode).containsExactly("SUCCESS_CODE", "FAILED_CODE");
        assertThat(manager.listLogs("SUCCESS_CODE")).extracting(A2aLogResponse::getTaskCode).containsExactly("SUCCESS_CODE");
        assertThat(manager.listLogs(" ")).hasSize(1);

        A2aStatisticsResponse statistics = manager.statistics();
        assertThat(statistics.getAgentCount()).isEqualTo(2);
        assertThat(statistics.getPublishedAgentCount()).isEqualTo(1);
        assertThat(statistics.getRouteCount()).isEqualTo(1);
        assertThat(statistics.getTaskCount()).isEqualTo(2);
        assertThat(statistics.getSuccessTaskCount()).isEqualTo(1);
        assertThat(statistics.getFailedTaskCount()).isEqualTo(1);
        assertThat(statistics.getLogCount()).isEqualTo(1);
    }

    private A2aAgentCardSaveRequest agentRequest() {
        A2aAgentCardSaveRequest request = new A2aAgentCardSaveRequest();
        request.setAgentCode("agent-code");
        request.setAgentName("agent-name");
        request.setEndpointUrl("http://agent");
        return request;
    }

    private A2aRouteSaveRequest routeRequest() {
        A2aRouteSaveRequest request = new A2aRouteSaveRequest();
        request.setRouteCode("route-code");
        request.setTargetAgentCode("target-agent");
        request.setTaskType("chat-analysis");
        return request;
    }

    private A2aDispatchRequest dispatchRequest() {
        A2aDispatchRequest request = new A2aDispatchRequest();
        request.setSourceAgentCode("source-agent");
        request.setTaskType("chat-analysis");
        request.setPayload(Map.of("input", "hello"));
        return request;
    }

    private A2aAgentCardRecord draftAgent(Long id, Long tenantId, Integer deletedFlag) {
        A2aAgentCardRecord record = publishedAgent("agent-" + id, "http://agent-" + id);
        record.setId(id);
        record.setTenantId(tenantId);
        record.setDeletedFlag(deletedFlag);
        record.setPublishStatus(A2aManagementConstants.PUBLISH_DRAFT);
        record.setAgentStatus(A2aManagementConstants.STATUS_DISABLED);
        record.setExt(commonJsonUtils.toJson(Map.of("dispatchConfig", Map.of("retryTimes", 1))));
        record.setCreateTime(LocalDateTime.of(2026, 1, 1, 1, 1));
        record.setUpdateTime(LocalDateTime.of(2026, 1, 2, 1, 1));
        return record;
    }

    private A2aAgentCardRecord publishedAgent(String agentCode, String endpointUrl) {
        A2aAgentCardRecord record = new A2aAgentCardRecord();
        record.setAgentCode(agentCode);
        record.setAgentName(agentCode + " name");
        record.setEndpointUrl(endpointUrl);
        record.setProtocolVersion("1.0");
        record.setTransportType("HTTP");
        record.setAuthType("NONE");
        record.setAgentStatus(A2aManagementConstants.STATUS_ENABLED);
        record.setPublishStatus(A2aManagementConstants.PUBLISH_PUBLISHED);
        record.setRiskLevel("LOW");
        record.setTrustLevel("INTERNAL");
        record.setTimeoutMs(10000);
        record.setRateLimitQps(10);
        record.setSuccessRateSlo(99);
        record.setTenantId(TENANT_ID);
        record.setDeletedFlag(0);
        record.setExt(commonJsonUtils.toJson(Map.of("dispatchConfig", Map.of("retryTimes", 0))));
        return record;
    }

    private A2aRouteRecord route(String routeCode, String targetAgentCode, String fallbackAgentCodes) {
        A2aRouteRecord record = new A2aRouteRecord();
        record.setRouteCode(routeCode);
        record.setRouteName(routeCode + " name");
        record.setSourceAgentCode("source-agent");
        record.setTargetAgentCode(targetAgentCode);
        record.setTaskType("chat-analysis");
        record.setRouteStatus(A2aManagementConstants.STATUS_ENABLED);
        record.setPriorityNo(10);
        record.setFailoverEnabled(fallbackAgentCodes == null ? 0 : 1);
        record.setFallbackAgentCodes(fallbackAgentCodes);
        record.setTenantId(TENANT_ID);
        return record;
    }

    private A2aTaskRecord task(String taskCode, String status) {
        A2aTaskRecord record = new A2aTaskRecord();
        record.setTaskCode(taskCode);
        record.setTaskType("chat-analysis");
        record.setSourceAgentCode("source-agent");
        record.setTargetAgentCode("target-agent");
        record.setRouteCode("route");
        record.setTaskStatus(status);
        record.setRequestPayloadJson("{}");
        record.setResponsePayloadJson("{}");
        record.setTenantId(TENANT_ID);
        return record;
    }

    private A2aExecutionLogRecord log(String taskCode) {
        A2aExecutionLogRecord record = new A2aExecutionLogRecord();
        record.setTaskCode(taskCode);
        record.setTraceId("trace");
        record.setSourceAgentCode("source-agent");
        record.setTargetAgentCode("target-agent");
        record.setRouteCode("route");
        record.setEventType(A2aManagementConstants.EVENT_DISPATCH);
        record.setExecuteStatus(A2aManagementConstants.TASK_SUCCESS);
        record.setAttemptNo(1);
        record.setRetryIndex(0);
        record.setSuccessFlag(1);
        record.setElapsedMs(10L);
        record.setTenantId(TENANT_ID);
        return record;
    }
}



