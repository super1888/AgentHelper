package com.spring.ai.vectorstore.exception;

import org.springframework.http.HttpStatus;

/**
 * 向量库模块业务异常。
 * 用于携带业务错误信息以及对应的 HTTP 状态码。
 */
public class VectorStoreException extends RuntimeException {

    private final HttpStatus httpStatus;

    public VectorStoreException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public VectorStoreException(HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * 创建 400 业务异常。
     *
     * @param message 错误信息
     * @return 异常对象
     */
    public static VectorStoreException badRequest(String message) {
        return new VectorStoreException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 创建 500 业务异常。
     *
     * @param message 错误信息
     * @param cause 原始异常
     * @return 异常对象
     */
    public static VectorStoreException internalError(String message, Throwable cause) {
        return new VectorStoreException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
