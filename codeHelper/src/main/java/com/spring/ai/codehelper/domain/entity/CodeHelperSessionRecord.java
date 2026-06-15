package com.spring.ai.codehelper.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码助手会话实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_helper_session")
public class CodeHelperSessionRecord extends BaseEntity {

    @TableField("session_code")
    private String sessionCode;

    @TableField("session_name")
    private String sessionName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;

    @TableField("workspace_path")
    private String workspacePath;

    @TableField("project_name")
    private String projectName;

    @TableField("branch_name")
    private String branchName;

    @TableField("task_description")
    private String taskDescription;

    @TableField("model_code")
    private String modelCode;

    @TableField("session_status")
    private String sessionStatus;

    @TableField("summary_snapshot")
    private String summarySnapshot;

    @TableField("allowed_commands_json")
    private String allowedCommandsJson;
}
