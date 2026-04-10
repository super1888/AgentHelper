package com.spring.ai.common.web;

import com.spring.ai.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 统一接口响应体。
 *
 * @param <T> 响应数据类型
 */
@Getter
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = ErrorCodeEnum.SUCCESS.getCode();
        response.message = ErrorCodeEnum.SUCCESS.getMessage();
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = success(data);
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> fail(ErrorCodeEnum errorCodeEnum, String message) {
        return fail(errorCodeEnum.getCode(), message);
    }

}
