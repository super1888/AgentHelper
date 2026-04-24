package com.spring.ai.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Model secret crypto properties.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.model-secret")
public class ModelSecretCryptoProperties {

    /**
     * Base64 encoded AES key. 16/24/32 bytes after decode.
     */
    private String aesKey;

}
