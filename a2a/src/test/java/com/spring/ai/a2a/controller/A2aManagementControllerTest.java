package com.spring.ai.a2a.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class A2aManagementControllerTest {

    @Mock
    private A2aApplicationManager manager;

    private A2aManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new A2aManagementController();
        ReflectionTestUtils.setField(controller, "a2aApplicationManager", manager);
    }

    @Test
    void delegatesAgentCardEndpointsAndWrapsSuccessResponse() {
        A2aAgentCardResponse response = A2aAgentCardResponse.builder().id(1L).agentCode("agent").build();
        A2aAgentCardSaveRequest request = new A2aAgentCardSaveRequest();
        when(manager.listAgentCards()).thenReturn(List.of(response));
        when(manager.listDeletedAgentCards()).thenReturn(List.of(response));
        when(manager.saveAgentCard(request)).thenReturn(response);
        when(manager.publishAgentCard(1L)).thenReturn(response);
        when(manager.restoreAgentCard(1L)).thenReturn(response);

        assertSuccessData(controller.listAgentCards(), List.of(response));
        assertSuccessData(controller.listDeletedAgentCards(), List.of(response));
        assertSuccessData(controller.saveAgentCard(request), response);
        assertSuccessData(controller.publishAgentCard(1L), response);
        assertSuccessData(controller.restoreAgentCard(1L), response);
        assertSuccessData(controller.deleteAgentCard(1L), null);
        verify(manager).deleteAgentCard(1L);
    }

    @Test
    void delegatesRouteDispatchTaskLogAndStatisticsEndpoints() {
        A2aRouteSaveRequest routeRequest = new A2aRouteSaveRequest();
        A2aDispatchRequest dispatchRequest = new A2aDispatchRequest();
        A2aRouteResponse routeResponse = A2aRouteResponse.builder().routeCode("route").build();
        A2aTaskResponse taskResponse = A2aTaskResponse.builder().taskCode("task").build();
        A2aLogResponse logResponse = A2aLogResponse.builder().taskCode("task").build();
        A2aStatisticsResponse statistics = A2aStatisticsResponse.builder().agentCount(1).build();
        when(manager.listRoutes()).thenReturn(List.of(routeResponse));
        when(manager.saveRoute(routeRequest)).thenReturn(routeResponse);
        when(manager.dispatch(dispatchRequest)).thenReturn(taskResponse);
        when(manager.listTasks()).thenReturn(List.of(taskResponse));
        when(manager.listLogs("task")).thenReturn(List.of(logResponse));
        when(manager.statistics()).thenReturn(statistics);

        assertSuccessData(controller.listRoutes(), List.of(routeResponse));
        assertSuccessData(controller.saveRoute(routeRequest), routeResponse);
        assertSuccessData(controller.dispatch(dispatchRequest), taskResponse);
        assertSuccessData(controller.listTasks(), List.of(taskResponse));
        assertSuccessData(controller.listLogs("task"), List.of(logResponse));
        assertSuccessData(controller.statistics(), statistics);
    }

    private void assertSuccessData(ApiResponse<?> response, Object expectedData) {
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(expectedData);
    }
}
