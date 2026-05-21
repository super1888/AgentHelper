package com.spring.ai.common.exception;

import com.spring.ai.common.enums.ErrorCodeEnum;
import org.springframework.http.HttpStatus;

/**
 * 文件用途：业务异常快捷工厂
 * 核心职责：统一构造常用的 400/404/409 异常，减少各模块重复拼装异常对象
 */
public final class BusinessExceptions {

    private BusinessExceptions() {
    }

    /**
     * 构造 400 参数错误异常。
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 构造 404 资源不存在异常。
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

}
