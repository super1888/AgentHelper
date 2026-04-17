package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 管理台账实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_record")
public class SkillRecord extends BaseEntity {

    @TableField("skill_code")
    private String skillCode;

    @TableField("skill_name")
    private String skillName;

    @TableField("description")
    private String description;

    @TableField("skill_category")
    private String skillCategory;

    @TableField("skill_status")
    private String skillStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("version_mode")
    private String versionMode;

    @TableField("current_version_no")
    private Integer currentVersionNo;

    @TableField("latest_version_no")
    private Integer latestVersionNo;

    @TableField("published_version_no")
    private Integer publishedVersionNo;

    @TableField("hot_update_enabled")
    private Integer hotUpdateEnabled;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
