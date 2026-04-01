package com.spring.ai.common.utils;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
public class BaseUtils {

    /**
     * 空值默认工具方法
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

}
