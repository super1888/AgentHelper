package com.spring.ai.flowable.service.impl;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.flowable.domain.request.FlowableApprovalDecisionRequest;
import com.spring.ai.flowable.domain.request.FlowableApprovalStartRequest;
import com.spring.ai.flowable.domain.response.FlowableApprovalDetailResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalHistoryTaskResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalStartResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalTaskResponse;
import com.spring.ai.flowable.service.FlowableApprovalService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Flowable 审批流程服务实现。
 */
@Service
public class FlowableApprovalServiceImpl implements FlowableApprovalService {

    private static final String PROCESS_DEFINITION_KEY = "approvalProcess";
    private static final String VARIABLE_APPROVED = "approved";
    private static final String VARIABLE_COMMENT = "approvalComment";
    private static final String VARIABLE_OPERATOR_ID = "approvalOperatorId";
    private static final String VARIABLE_OPERATOR_NAME = "approvalOperatorName";
    private static final String VARIABLE_APPROVAL_TIME = "approvalTime";

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    /**
     * 发起审批流程。
     */
    @Override
    public FlowableApprovalStartResponse startWorkflow(FlowableApprovalStartRequest request) {
        validateStartRequest(request);
        Map<String, Object> variables = buildStartVariables(request);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY,
                request.getBusinessKey(),
                variables
        );

        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        FlowableApprovalStartResponse response = new FlowableApprovalStartResponse();
        response.setProcessInstanceId(processInstance.getId());
        response.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        response.setBusinessKey(request.getBusinessKey());
        response.setCurrentTaskId(currentTask == null ? null : currentTask.getId());
        response.setCurrentTaskName(currentTask == null ? null : currentTask.getName());
        response.setCurrentTaskAssignee(currentTask == null ? null : currentTask.getAssignee());
        response.setProcessStatus("RUNNING");
        return response;
    }

    /**
     * 处理审批动作。
     */
    @Override
    public FlowableApprovalDetailResponse decide(String taskId, FlowableApprovalDecisionRequest request) {
        if (!StringUtils.hasText(taskId)) {
            throw BusinessExceptions.badRequest("任务ID不能为空");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BusinessExceptions.notFound("未找到待审批任务");
        }
        if (StringUtils.hasText(task.getAssignee()) && !Objects.equals(task.getAssignee(), request.getOperatorId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, HttpStatus.FORBIDDEN, "当前用户不是该任务的处理人");
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(VARIABLE_APPROVED, request.getApproved());
        variables.put(VARIABLE_COMMENT, request.getComment());
        variables.put(VARIABLE_OPERATOR_ID, request.getOperatorId());
        variables.put(VARIABLE_OPERATOR_NAME, request.getOperatorName());
        variables.put(VARIABLE_APPROVAL_TIME, new Date());

        taskService.addComment(task.getId(), task.getProcessInstanceId(), request.getComment());
        taskService.complete(task.getId(), variables);
        return getWorkflowDetail(task.getProcessInstanceId());
    }

    /**
     * 查询待办任务。
     */
    @Override
    public List<FlowableApprovalTaskResponse> listTodoTasks(String assignee) {
        TaskQuery query = taskService.createTaskQuery().active().orderByTaskCreateTime().desc();
        if (StringUtils.hasText(assignee)) {
            query.taskAssignee(assignee);
        }
        List<Task> tasks = query.list();
        List<FlowableApprovalTaskResponse> responses = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            responses.add(toTaskResponse(task));
        }
        return responses;
    }

    /**
     * 查询流程详情。
     */
    @Override
    public FlowableApprovalDetailResponse getWorkflowDetail(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            throw BusinessExceptions.badRequest("流程实例ID不能为空");
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicProcessInstance == null) {
            throw BusinessExceptions.notFound("未找到流程实例");
        }

        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();

        FlowableApprovalDetailResponse response = new FlowableApprovalDetailResponse();
        response.setProcessInstanceId(processInstanceId);
        response.setProcessDefinitionId(historicProcessInstance.getProcessDefinitionId());
        response.setProcessDefinitionKey(historicProcessInstance.getProcessDefinitionKey());
        response.setBusinessKey(historicProcessInstance.getBusinessKey());
        response.setStartTime(historicProcessInstance.getStartTime());
        response.setEndTime(historicProcessInstance.getEndTime());
        response.setProcessStatus(historicProcessInstance.getEndTime() == null ? "RUNNING" : "FINISHED");
        response.setVariables(resolveVariables(processInstanceId));
        response.setCurrentTasks(currentTasks.stream().map(this::toTaskResponse).toList());
        response.setHistoryTasks(historicTasks.stream().map(this::toHistoryTaskResponse).toList());
        return response;
    }

    private void validateStartRequest(FlowableApprovalStartRequest request) {
        if (request.getAmount() != null && request.getAmount().signum() < 0) {
            throw BusinessExceptions.badRequest("金额不能为负数");
        }
        if (request.getLeaveDays() != null && request.getLeaveDays() <= 0) {
            throw BusinessExceptions.badRequest("请假天数必须大于0");
        }
    }

    private Map<String, Object> buildStartVariables(FlowableApprovalStartRequest request) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessKey", request.getBusinessKey());
        variables.put("requestNo", request.getRequestNo());
        variables.put("title", request.getTitle());
        variables.put("applicantId", request.getApplicantId());
        variables.put("applicantName", request.getApplicantName());
        variables.put("approverUserId", request.getApproverUserId());
        variables.put("approverName", request.getApproverName());
        variables.put("department", request.getDepartment());
        variables.put("reason", request.getReason());
        variables.put("amount", request.getAmount());
        variables.put("leaveDays", request.getLeaveDays());
        return variables;
    }

    private FlowableApprovalTaskResponse toTaskResponse(Task task) {
        FlowableApprovalTaskResponse response = new FlowableApprovalTaskResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setAssignee(task.getAssignee());
        response.setProcessInstanceId(task.getProcessInstanceId());
        response.setProcessDefinitionId(task.getProcessDefinitionId());
        response.setBusinessKey(resolveBusinessKey(task.getProcessInstanceId()));
        response.setCreateTime(task.getCreateTime());
        return response;
    }

    private FlowableApprovalHistoryTaskResponse toHistoryTaskResponse(HistoricTaskInstance task) {
        FlowableApprovalHistoryTaskResponse response = new FlowableApprovalHistoryTaskResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setAssignee(task.getAssignee());
        response.setStartTime(task.getStartTime());
        response.setEndTime(task.getEndTime());
        response.setDeleteReason(task.getDeleteReason());
        return response;
    }

    private Map<String, Object> resolveVariables(String processInstanceId) {
        ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId);
        if (runtimeQuery.singleResult() != null) {
            return new LinkedHashMap<>(runtimeService.getVariables(processInstanceId));
        }

        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        Map<String, Object> resolvedVariables = new LinkedHashMap<>();
        for (HistoricVariableInstance variable : variables) {
            resolvedVariables.put(variable.getVariableName(), variable.getValue());
        }
        return resolvedVariables;
    }

    private String resolveBusinessKey(String processInstanceId) {
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtimeInstance != null) {
            return runtimeInstance.getBusinessKey();
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return historicProcessInstance == null ? null : historicProcessInstance.getBusinessKey();
    }
}
