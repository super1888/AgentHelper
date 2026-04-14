package com.spring.ai.common.exception;

import lombok.Getter;

/**
 * 枚举类业务异常。
 *
 * @author zhuoqi
 */
@Getter
public class EnumException extends RuntimeException {

    private String message;

    public EnumException(String message) {
        super(message);
        this.message = message;
    }

    public EnumException(Throwable cause) {
        super(cause);
    }

}
