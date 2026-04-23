package com.spring.ai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Model secret crypto properties.
 */
@Component
@ConfigurationProperties(prefix = "agent-helper.model-secret")
public class ModelSecretCryptoProperties {

    /**
     * Base64 encoded AES key. 16/24/32 bytes after decode.
     */
    private String aesKey;

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
