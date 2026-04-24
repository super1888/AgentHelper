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

@Component
public class CommonJsonUtils {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private ObjectMapper objectMapper;

    public Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON parse failed", ex);
        }
    }

    public <T> T parseObject(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON parse failed", ex);
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON serialize failed", ex);
        }
    }

    public String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON pretty print failed", ex);
        }
    }

    /**
     * 将 JSON 文本规范化为紧凑格式，并在入参非法时抛出可读业务异常。
     */
    public String normalizeJsonOrNull(String rawJson, String fieldName) {
        String value = CommonTextUtils.trimToNull(rawJson);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(value));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 不是合法 JSON");
        }
    }

    public Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map)) {
            return Map.of();
        }
        Map<?, ?> map = (Map<?, ?>) value;
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));
    }

    public Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }
}
