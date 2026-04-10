package com.spring.ai.interceptors.factory;

import com.spring.ai.common.enums.InterceptorTypeEnum;

/**
 * Interceptor 创建器接口。
 */
public interface InterceptorCreator {

    /**
     * 获取当前创建器支持的拦截器类型。
     *
     * @return 拦截器类型
     */
    InterceptorTypeEnum getInterceptorType();

    /**
     * 创建拦截器对象。
     *
     * @param dto 构建参数
     * @return 拦截器对象
     */
    Object create(Object dto);
}
