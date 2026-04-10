package com.spring.ai.vectorstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库模块配置参数。
 * 主要用于控制检索数量、切片策略、文本清洗策略等行为。
 */
@ConfigurationProperties(prefix = "app.vector-store")
public class VectorStoreProperties {

    /**
     * 默认返回结果数量。
     */
    private int defaultTopK = 3;

    /**
     * 单个切片的目标 token 数。
     */
    private int chunkSize = 800;

    /**
     * 单个切片最小字符数。
     */
    private int minChunkSizeChars = 350;

    /**
     * 可参与向量化的最小切片长度。
     */
    private int minChunkLengthToEmbed = 5;

    /**
     * 单篇文档允许切分出的最大切片数量。
     */
    private int maxNumChunks = 10000;

    /**
     * 切片时是否保留原始分隔符。
     */
    private boolean keepSeparator = true;

    /**
     * 是否将文本左对齐。
     */
    private boolean leftAlignment = false;

    /**
     * 删除页眉页脚前，跳过的顶部页数。
     */
    private int numberOfTopPagesToSkipBeforeDelete = 0;

    /**
     * 删除顶部文本行数。
     */
    private int numberOfTopTextLinesToDelete = 0;

    /**
     * 删除底部文本行数。
     */
    private int numberOfBottomTextLinesToDelete = 0;

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getMinChunkSizeChars() {
        return minChunkSizeChars;
    }

    public void setMinChunkSizeChars(int minChunkSizeChars) {
        this.minChunkSizeChars = minChunkSizeChars;
    }

    public int getMinChunkLengthToEmbed() {
        return minChunkLengthToEmbed;
    }

    public void setMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
    }

    public int getMaxNumChunks() {
        return maxNumChunks;
    }

    public void setMaxNumChunks(int maxNumChunks) {
        this.maxNumChunks = maxNumChunks;
    }

    public boolean isKeepSeparator() {
        return keepSeparator;
    }

    public void setKeepSeparator(boolean keepSeparator) {
        this.keepSeparator = keepSeparator;
    }

    public boolean isLeftAlignment() {
        return leftAlignment;
    }

    public void setLeftAlignment(boolean leftAlignment) {
        this.leftAlignment = leftAlignment;
    }

    public int getNumberOfTopPagesToSkipBeforeDelete() {
        return numberOfTopPagesToSkipBeforeDelete;
    }

    public void setNumberOfTopPagesToSkipBeforeDelete(int numberOfTopPagesToSkipBeforeDelete) {
        this.numberOfTopPagesToSkipBeforeDelete = numberOfTopPagesToSkipBeforeDelete;
    }

    public int getNumberOfTopTextLinesToDelete() {
        return numberOfTopTextLinesToDelete;
    }

    public void setNumberOfTopTextLinesToDelete(int numberOfTopTextLinesToDelete) {
        this.numberOfTopTextLinesToDelete = numberOfTopTextLinesToDelete;
    }

    public int getNumberOfBottomTextLinesToDelete() {
        return numberOfBottomTextLinesToDelete;
    }

    public void setNumberOfBottomTextLinesToDelete(int numberOfBottomTextLinesToDelete) {
        this.numberOfBottomTextLinesToDelete = numberOfBottomTextLinesToDelete;
    }
}
