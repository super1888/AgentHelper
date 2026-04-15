package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 定义表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent")
public class SyAgent extends BaseEntity {

    @TableField("agent_code")
    private String agentCode;

    @TableField("agent_name")
    private String agentName;

    @TableField("description")
    private String description;

    @TableField("agent_type")
    private String agentType;

    @TableField("agent_status")
    private String agentStatus;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;

    @TableField("current_version_id")
    private Long currentVersionId;

    @TableField("published_version_id")
    private Long publishedVersionId;

    @TableField("published_version_no")
    private Integer publishedVersionNo;

    @TableField("latest_version_no")
    private Integer latestVersionNo;
}
