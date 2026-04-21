package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Interceptor 版本快照实体
 * 核心职责：保存拦截器配置的完整版本历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interceptor_version_record")
public class InterceptorVersionRecord extends BaseEntity {

    @TableField("interceptor_id")
    private Long interceptorId;

    @TableField("interceptor_code")
    private String interceptorCode;

    @TableField("interceptor_name")
    private String interceptorName;

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
