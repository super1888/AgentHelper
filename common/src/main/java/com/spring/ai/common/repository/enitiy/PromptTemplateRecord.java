package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提示词模板台账实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prompt_template")
public class PromptTemplateRecord extends BaseEntity {

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("description")
    private String description;

    @TableField("template_type")
    private String templateType;

    @TableField("source_type")
    private String sourceType;

    @TableField("template_content")
    private String templateContent;

    @TableField("source_path")
    private String sourcePath;

    @TableField("template_status")
    private String templateStatus;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
