package com.spring.ai.core.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingModelConfig {

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(
            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel dashscopeEmbeddingModel) {
        return dashscopeEmbeddingModel;
    }
}
