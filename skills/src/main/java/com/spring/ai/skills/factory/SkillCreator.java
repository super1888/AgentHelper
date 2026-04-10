package com.spring.ai.skills.factory;

import com.spring.ai.common.enums.SkillFactoryTypeEnum;

/**
 * Skills 创建器接口。
 */
public interface SkillCreator {

    /**
     * 获取当前创建器支持的类型。
     *
     * @return 类型枚举
     */
    SkillFactoryTypeEnum getSkillFactoryType();

    /**
     * 创建对象。
     *
     * @param dto 入参 DTO
     * @return 创建后的对象
     */
    Object create(Object dto);
}
