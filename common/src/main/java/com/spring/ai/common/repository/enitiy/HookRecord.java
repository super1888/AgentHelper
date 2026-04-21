package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Hook 管理主表实体
 * 核心职责：承载 Hook 的基础属性、发布状态、执行策略和归属信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hook_record")
public class HookRecord extends BaseEntity {

    @TableField("hook_code")
    private String hookCode;

    @TableField("hook_name")
    private String hookName;

    @TableField("description")
    private String description;

    @TableField("hook_type")
    private String hookType;

    @TableField("hook_stage")
    private String hookStage;

    @TableField("hook_status")
    private String hookStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("trigger_mode")
    private String triggerMode;

    @TableField("fail_strategy")
    private String failStrategy;

    @TableField("sort_weight")
    private Integer sortWeight;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("hot_update_enabled")
    private Integer hotUpdateEnabled;

    @TableField("current_version_no")
    private Integer currentVersionNo;

    @TableField("latest_version_no")
    private Integer latestVersionNo;

    @TableField("published_version_no")
    private Integer publishedVersionNo;

    @TableField("builtin_hook_key")
    private String builtinHookKey;

    @TableField("script_language")
    private String scriptLanguage;

    @TableField("deleted_flag")
    private Integer deletedFlag;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
