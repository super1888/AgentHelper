package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Hook 版本快照实体
 * 核心职责：保存 Hook 配置的完整版本历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hook_version_record")
public class HookVersionRecord extends BaseEntity {

    @TableField("hook_id")
    private Long hookId;

    @TableField("hook_code")
    private String hookCode;

    @TableField("hook_name")
    private String hookName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("version_code")
    private String versionCode;

    @TableField("version_description")
    private String versionDescription;

    @TableField("version_status")
    private String versionStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("snapshot_json")
    private String snapshotJson;
}
