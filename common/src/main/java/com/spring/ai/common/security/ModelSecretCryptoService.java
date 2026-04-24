package com.spring.ai.common.security;

import com.spring.ai.common.config.ModelSecretCryptoProperties;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Encrypts model provider secrets with AES/GCM.
 */
@Component
public class ModelSecretCryptoService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @Resource
    private ModelSecretCryptoProperties properties;

    /**
     * Encrypt plain text secret.
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plainText.trim().getBytes(StandardCharsets.UTF_8));
            byte[] merged = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, merged, 0, iv.length);
            System.arraycopy(encrypted, 0, merged, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(merged);
        } catch (GeneralSecurityException ex) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "模型密钥加密失败",
                    ex
            );
        }
    }

    /**
     * Decrypt stored secret.
     */
    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        try {
            byte[] merged = Base64.getDecoder().decode(cipherText);
            if (merged.length <= IV_LENGTH) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型密钥密文格式不正确");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[merged.length - IV_LENGTH];
            System.arraycopy(merged, 0, iv, 0, IV_LENGTH);
            System.arraycopy(merged, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "模型密钥解密失败",
                    ex
            );
        }
    }

    private SecretKeySpec buildKey() {
        String aesKey = properties.getAesKey();
        if (!StringUtils.hasText(aesKey)) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "未配置模型密钥加密主密钥");
        }
        byte[] decoded = Base64.getDecoder().decode(aesKey.trim());
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "模型密钥主密钥长度不合法");
        }
        return new SecretKeySpec(decoded, AES);
    }
}
