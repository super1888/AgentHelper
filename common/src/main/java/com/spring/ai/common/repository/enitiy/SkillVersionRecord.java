package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 版本快照实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_version_record")
public class SkillVersionRecord extends BaseEntity {

    @TableField("skill_id")
    private Long skillId;

    @TableField("skill_code")
    private String skillCode;

    @TableField("skill_name")
    private String skillName;

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

    @TableField("release_stage")
    private String releaseStage;

    @TableField("snapshot_json")
    private String snapshotJson;
}
