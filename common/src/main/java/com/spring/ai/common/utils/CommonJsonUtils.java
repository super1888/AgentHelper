package com.spring.ai.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：通用 JSON 工具
 * 核心职责：统一处理 JSON 序列化、反序列化与 Map 类型转换，减少各模块重复实现
 */
@Component
public class CommonJsonUtils {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 解析 JSON 字符串为 Map。
     */
    public Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON parse failed", e);
        }
    }

    /**
     * 将 JSON 字符串解析为指定对象。
     */
    public <T> T parseObject(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON parse failed", e);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON serialize failed", e);
        }
    }

    /**
     * 生成格式化 JSON 字符串，便于调试展示。
     */
    public String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "JSON pretty print failed", e);
        }
    }

    /**
     * 将任意对象安全转换为 Map 结构。
     */
    public Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(Collectors.toMap(item -> String.valueOf(item.getKey()), Map.Entry::getValue));
        }
        return Map.of();
    }

    /**
     * Map 空值保护。
     */
    public Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }
}
