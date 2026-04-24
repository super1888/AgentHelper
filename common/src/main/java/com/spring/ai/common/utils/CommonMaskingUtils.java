package com.spring.ai.common.utils;

import org.springframework.util.StringUtils;

/**
 * 通用脱敏工具。
 */
public final class CommonMaskingUtils {

    private CommonMaskingUtils() {
    }

    /**
     * 仅保留尾部指定长度，其余部分全部脱敏。
     */
    public static String maskKeepTail(String value, int visibleTailLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int safeVisibleLength = Math.max(0, visibleTailLength);
        int visible = Math.min(safeVisibleLength, value.length());
        return "*".repeat(Math.max(0, value.length() - visible)) + value.substring(value.length() - visible);
    }
}
