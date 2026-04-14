package com.spring.quickstart.config.mybatis.generator;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.spring.ai.common.id.SnowflakeIdGenerator;

/**
 * MyBatis-Plus 主键生成器适配器。
 */
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SnowflakeIdentifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Number nextId(Object entity) {
        return snowflakeIdGenerator.nextId();
    }
}
