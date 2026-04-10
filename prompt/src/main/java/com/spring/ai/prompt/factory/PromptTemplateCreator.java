package com.spring.ai.prompt.factory;

import com.spring.ai.common.enums.PromptTemplateTypeEnum;

/**
 * Prompt 模板创建器接口。
 */
public interface PromptTemplateCreator {

    /**
     * 获取当前创建器支持的模板类型。
     *
     * @return 模板类型
     */
    PromptTemplateTypeEnum getPromptTemplateType();

    /**
     * 创建模板对象。
     *
     * @param dto 构建参数
     * @return 模板对象
     */
    Object create(Object dto);
}
