package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 主档实体。
 *
 * <p>保存 Agent 的基础信息、归属信息以及当前发布版本指针。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent")
public class SyAgent extends BaseEntity {

    /**
     * Agent 业务编码，对外暴露使用。
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * Agent 名称。
     */
    @TableField("agent_name")
    private String agentName;

    /**
     * Agent 描述。
     */
    @TableField("description")
    private String description;

    /**
     * Agent 类型。
     */
    @TableField("agent_type")
    private String agentType;

    /**
     * Agent 当前状态，例如草稿、已发布、已禁用。
     */
    @TableField("agent_status")
    private String agentStatus;

    /**
     * 所属租户 ID。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 所属用户 ID。
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 所属用户名。
     */
    @TableField("owner_user_name")
    private String ownerUserName;

    /**
     * 当前编辑版本主键。
     */
    @TableField("current_version_id")
    private Long currentVersionId;

    /**
     * 当前已发布版本主键。
     */
    @TableField("published_version_id")
    private Long publishedVersionId;

    /**
     * 当前已发布版本号。
     */
    @TableField("published_version_no")
    private Integer publishedVersionNo;

    /**
     * 最新版本号。
     */
    @TableField("latest_version_no")
    private Integer latestVersionNo;
}
