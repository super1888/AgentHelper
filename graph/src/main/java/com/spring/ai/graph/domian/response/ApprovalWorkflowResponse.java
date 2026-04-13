package com.spring.ai.graph.domian.response;

import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 审批工作流统一响应。
 *
 * <p>把线程 ID、当前节点、状态和时间线一起返回，方便你观察 Graph 在每次执行后的状态快照。</p>
 */
@Data
public class ApprovalWorkflowResponse {

    private String threadId;
    private String nextNode;
    private String currentNode;
    private String status;
    private boolean interrupted;
    private ApprovalWorkflowCheckpointModeEnum checkpointType;
    private Map<String, Object> state;
    private List<String> timeline;

}
