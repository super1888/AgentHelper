package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 配置版本表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_version")
public class SyAgentVersion extends BaseEntity {

    @TableField("agent_id")
    private Long agentId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("agent_name")
    private String agentName;

    @TableField("description")
    private String description;

    @TableField("system_prompt")
    private String systemPrompt;

    @TableField("selected_capabilities_json")
    private String selectedCapabilitiesJson;

    @TableField("config_snapshot_json")
    private String configSnapshotJson;

    @TableField("is_published")
    private Integer isPublished;
}
