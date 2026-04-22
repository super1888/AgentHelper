package com.spring.ai.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 文件用途：通用文本与集合处理工具
 * 核心职责：统一处理 trim、空值转换、字符串列表清洗与时间戳转换
 */
public final class CommonTextUtils {

    private CommonTextUtils() {
    }

    /**
     * 返回带默认值的文本。
     */
    public static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 去除前后空格。
     */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 去除前后空格，空白字符串返回 null。
     */
    public static String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    /**
     * 清洗字符串列表，空值过滤并去重。
     */
    public static List<String> emptyIfNull(List<String> value) {
        return value == null ? List.of() : value.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 将任意列表对象转为字符串列表。
     */
    public static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StringUtils::hasText).map(String::trim).toList();
        }
        return List.of();
    }

    /**
     * LocalDateTime 转毫秒时间戳。
     */
    public static Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
