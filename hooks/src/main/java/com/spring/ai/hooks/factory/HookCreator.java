package com.spring.ai.hooks.factory;

import com.spring.ai.common.enums.HookTypeEnum;

/**
 * Hook 创建器接口。
 */
public interface HookCreator {

    /**
     * 获取当前创建器支持的 Hook 类型。
     *
     * @return Hook 类型
     */
    HookTypeEnum getHookType();

    /**
     * 创建 Hook 对象。
     *
     * @param dto 构建参数
     * @return Hook 对象
     */
    Object create(Object dto);
}
