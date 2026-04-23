package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model definition entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_definition")
public class ModelDefinition extends BaseEntity {

    @TableField("model_code")
    private String modelCode;

    @TableField("model_name")
    private String modelName;

    @TableField("provider_config_id")
    private Long providerConfigId;

    @TableField("provider_config_code")
    private String providerConfigCode;

    @TableField("provider_enum")
    private String providerEnum;

    @TableField("model_type")
    private String modelType;

    @TableField("model_identifier")
    private String modelIdentifier;

    @TableField("temperature")
    private Double temperature;

    @TableField("top_p")
    private Double topP;

    @TableField("presence_penalty")
    private Double presencePenalty;

    @TableField("frequency_penalty")
    private Double frequencyPenalty;

    @TableField("max_tokens")
    private Integer maxTokens;

    @TableField("context_window")
    private Integer contextWindow;

    @TableField("rpm_limit")
    private Integer rpmLimit;

    @TableField("tpm_limit")
    private Integer tpmLimit;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("support_streaming")
    private Integer supportStreaming;

    @TableField("support_tools")
    private Integer supportTools;

    @TableField("support_vision")
    private Integer supportVision;

    @TableField("support_json_schema")
    private Integer supportJsonSchema;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;

    @TableField("advanced_config_json")
    private String advancedConfigJson;
}
