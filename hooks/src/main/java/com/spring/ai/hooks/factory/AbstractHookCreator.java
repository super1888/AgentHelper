package com.spring.ai.hooks.factory;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.common.exception.BusinessException;

/**
 * Hook 创建器抽象父类。
 */
public abstract class AbstractHookCreator implements HookCreator {

    @Override
    public HookTypeEnum getHookType() {
        throw new UnsupportedOperationException("子类必须实现获取钩子类型的方法");
    }

    @Override
    public Object create(Object dto) {
        throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前钩子创建器尚未实现");
    }
}
