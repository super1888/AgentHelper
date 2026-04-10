package com.spring.ai.vectorstore.exception;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 向量库模块业务异常。 用于携带业务错误信息以及对应的 HTTP 状态码。
 *
 * @author zhuoqi
 */
public class VectorStoreException extends BusinessException {

    public VectorStoreException(HttpStatus httpStatus, String message) {
        super(ErrorCodeEnum.VECTOR_STORE_ERROR, httpStatus, message);
    }

    public VectorStoreException(HttpStatus httpStatus, String message, Throwable cause) {
        super(ErrorCodeEnum.VECTOR_STORE_ERROR, httpStatus, message, cause);
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
     * @param cause   原始异常
     * @return 异常对象
     */
    public static VectorStoreException internalError(String message, Throwable cause) {
        return new VectorStoreException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
