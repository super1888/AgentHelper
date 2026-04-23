package com.spring.ai.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.util.StringUtils;

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
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 将任意列表对象转为字符串列表。
     */
    public static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return List.of();
        }
        List<?> list = (List<?>) value;
        return list.stream()
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    public static Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
