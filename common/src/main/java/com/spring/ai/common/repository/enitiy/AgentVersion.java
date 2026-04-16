package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 版本快照实体。
 *
 * <p>每次保存配置都会落一条独立快照，用于发布、会话绑定和历史回溯。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_version")
public class AgentVersion extends BaseEntity {

    /**
     * 关联的 Agent 主档 ID。
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 所属租户 ID。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 版本号。
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 版本快照中的 Agent 名称。
     */
    @TableField("agent_name")
    private String agentName;

    /**
     * 版本快照中的 Agent 描述。
     */
    @TableField("description")
    private String description;

    /**
     * 系统提示词。
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 能力项快照 JSON。
     */
    @TableField("selected_capabilities_json")
    private String selectedCapabilitiesJson;

    /**
     * 完整配置快照 JSON。
     */
    @TableField("config_snapshot_json")
    private String configSnapshotJson;

    /**
     * 是否为已发布版本，1 表示已发布。
     */
    @TableField("is_published")
    private Integer isPublished;
}
