package com.spring.ai.common.exception;

import com.spring.ai.common.enums.ErrorCodeEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 通用业务异常。
 *
 * <p>适用于各业务模块抛出可预期异常，并携带统一错误码和 HTTP 状态码。</p>
 * @author zhuoqi
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        this(errorCodeEnum, errorCodeEnum.getMessage());
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum, String message) {
        this(errorCodeEnum, HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum, HttpStatus httpStatus, String message) {
        super(message);
        this.code = errorCodeEnum.getCode();
        this.httpStatus = httpStatus;
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCodeEnum.getCode();
        this.httpStatus = httpStatus;
    }

}
