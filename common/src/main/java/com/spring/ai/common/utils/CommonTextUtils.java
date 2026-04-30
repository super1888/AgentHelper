package com.spring.ai.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
     * 判断文本中是否包含任意关键字。
     */
    public static boolean containsAnyKeyword(String source, List<String> keywords) {
        if (!StringUtils.hasText(source) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    /**
     * 从模型输出中提取 JSON 主体。
     */
    public static String extractJsonBody(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return null;
        }
        String content = rawContent.trim();
        if (content.startsWith("```")) {
            int firstLineBreak = content.indexOf('\n');
            if (firstLineBreak >= 0) {
                content = content.substring(firstLineBreak + 1);
            }
            int lastFence = content.lastIndexOf("```");
            if (lastFence >= 0) {
                content = content.substring(0, lastFence);
            }
            content = content.trim();
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
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

    /**
     * 将 null 与空字符串视为等价，便于做表单字段比较。
     */
    public static boolean equalsNullableBlank(String left, String right) {
        return (left == null || left.isEmpty()) ? (right == null || right.isEmpty()) : left.equals(right);
    }

    public static Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
