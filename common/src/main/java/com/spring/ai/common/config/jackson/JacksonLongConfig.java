package com.spring.ai.common.config.jackson;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一将 Long 序列化为字符串，避免前端 JavaScript 精度丢失。
 */
@Configuration
public class JacksonLongConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLongCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
