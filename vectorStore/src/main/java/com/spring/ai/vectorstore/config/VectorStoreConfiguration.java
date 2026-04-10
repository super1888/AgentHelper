package com.spring.ai.vectorstore.config;

import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量库模块配置类。
 * 统一注册文本格式化器和文本切片器，便于整个模块复用。
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfiguration {

    /**
     * 文本格式化器。
     * 用于在文档解析后对文本进行对齐、去头尾、空行清理等预处理。
     *
     * @param properties 模块配置
     * @return 文本格式化器
     */
    @Bean
    public ExtractedTextFormatter extractedTextFormatter(VectorStoreProperties properties) {
        return ExtractedTextFormatter.builder()
                .withLeftAlignment(properties.isLeftAlignment())
                .withNumberOfTopPagesToSkipBeforeDelete(properties.getNumberOfTopPagesToSkipBeforeDelete())
                .withNumberOfTopTextLinesToDelete(properties.getNumberOfTopTextLinesToDelete())
                .withNumberOfBottomTextLinesToDelete(properties.getNumberOfBottomTextLinesToDelete())
                .build();
    }

    /**
     * Token 级文本切片器。
     * 用于将长文档切分为适合向量化的小片段。
     *
     * @param properties 模块配置
     * @return 切片器
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter(VectorStoreProperties properties) {
        return TokenTextSplitter.builder()
                .withChunkSize(properties.getChunkSize())
                .withMinChunkSizeChars(properties.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(properties.getMinChunkLengthToEmbed())
                .withMaxNumChunks(properties.getMaxNumChunks())
                .withKeepSeparator(properties.isKeepSeparator())
                .build();
    }
}
