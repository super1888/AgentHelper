package com.spring.quickstart.workflow.approval.domain.response;

import java.util.List;
import java.util.Map;

/**
 * 审批工作流统一响应。
 *
 * <p>把线程 ID、当前节点、状态和时间线一起返回，方便你观察 Graph 在每次执行后的状态快照。</p>
 */
public class ApprovalWorkflowResponse {

    private String threadId;
    private String nextNode;
    private String currentNode;
    private String status;
    private boolean interrupted;
    private Map<String, Object> state;
    private List<String> timeline;

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getNextNode() {
        return nextNode;
    }

    public void setNextNode(String nextNode) {
        this.nextNode = nextNode;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    public List<String> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<String> timeline) {
        this.timeline = timeline;
    }
}
