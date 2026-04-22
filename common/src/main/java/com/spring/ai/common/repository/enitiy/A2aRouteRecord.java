package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：A2A 路由策略实体
 * 核心职责：定义任务类型、来源 Agent 与目标远程 Agent 的路由关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("a2a_route_record")
public class A2aRouteRecord extends BaseEntity {

    @TableField("route_code")
    private String routeCode;

    @TableField("route_name")
    private String routeName;

    @TableField("source_agent_code")
    private String sourceAgentCode;

    @TableField("target_agent_code")
    private String targetAgentCode;

    @TableField("task_type")
    private String taskType;

    @TableField("route_status")
    private String routeStatus;

    @TableField("priority_no")
    private Integer priorityNo;

    @TableField("failover_enabled")
    private Integer failoverEnabled;

    @TableField("fallback_agent_codes")
    private String fallbackAgentCodes;

    @TableField("tenant_id")
    private Long tenantId;
}
