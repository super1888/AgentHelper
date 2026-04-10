package com.spring.ai.tools.factory;

import com.spring.ai.common.enums.ToolFactoryTypeEnum;

/**
 * Tool 创建器接口。
 */
public interface ToolCreator {

    /**
     * 获取当前创建器支持的类型。
     *
     * @return 工具工厂类型
     */
    ToolFactoryTypeEnum getToolFactoryType();

    /**
     * 创建工具对象。
     *
     * @param dto 构建参数
     * @return 工具对象
     */
    Object create(Object dto);
}
