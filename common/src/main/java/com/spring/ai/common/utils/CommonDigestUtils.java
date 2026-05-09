package com.spring.ai.common.utils;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * 通用摘要工具类。
 */
public final class CommonDigestUtils {

    private CommonDigestUtils() {
    }

    /**
     * 对文本执行 SHA-256 摘要，返回十六进制字符串。
     */
    public static String sha256Hex(String content, String errorMessage) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage
            );
        }
    }
}
