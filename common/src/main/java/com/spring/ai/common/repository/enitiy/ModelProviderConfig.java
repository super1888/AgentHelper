package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model provider config entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_provider_config")
public class ModelProviderConfig extends BaseEntity {

    @TableField("provider_config_code")
    private String providerConfigCode;

    @TableField("provider_enum")
    private String providerEnum;

    @TableField("provider_name")
    private String providerName;

    @TableField("base_url")
    private String baseUrl;

    @TableField("api_key_cipher_text")
    private String apiKeyCipherText;

    @TableField("organization_id")
    private String organizationId;

    @TableField("default_headers_json")
    private String defaultHeadersJson;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
