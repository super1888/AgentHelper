package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Interceptor 管理主表实体
 * 核心职责：承载拦截器的基础属性、发布状态、执行策略和归属信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interceptor_record")
public class InterceptorRecord extends BaseEntity {

    @TableField("interceptor_code")
    private String interceptorCode;

    @TableField("interceptor_name")
    private String interceptorName;

    @TableField("description")
    private String description;

    @TableField("interceptor_type")
    private String interceptorType;

    @TableField("interceptor_stage")
    private String interceptorStage;

    @TableField("interceptor_status")
    private String interceptorStatus;

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

    @TableField("builtin_interceptor_key")
    private String builtinInterceptorKey;

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
