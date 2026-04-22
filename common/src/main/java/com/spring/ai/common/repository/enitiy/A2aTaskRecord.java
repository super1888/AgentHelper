package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：A2A 任务实体
 * 核心职责：记录跨 Agent 调用任务的输入、输出、状态和路由结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("a2a_task_record")
public class A2aTaskRecord extends BaseEntity {

    @TableField("task_code")
    private String taskCode;

    @TableField("task_type")
    private String taskType;

    @TableField("source_agent_code")
    private String sourceAgentCode;

    @TableField("target_agent_code")
    private String targetAgentCode;

    @TableField("route_code")
    private String routeCode;

    @TableField("task_status")
    private String taskStatus;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("elapsed_ms")
    private Long elapsedMs;

    @TableField("tenant_id")
    private Long tenantId;
}
