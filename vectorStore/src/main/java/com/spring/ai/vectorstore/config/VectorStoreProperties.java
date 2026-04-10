package com.spring.ai.vectorstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库模块配置参数。
 * 主要用于控制检索数量、切片策略、批量写入策略等行为。
 */
@ConfigurationProperties(prefix = "app.vector-store")
public class VectorStoreProperties {

    private int defaultTopK = 3;
    private int chunkSize = 800;
    private int minChunkSizeChars = 350;
    private int minChunkLengthToEmbed = 5;
    private int maxNumChunks = 128;
    private boolean keepSeparator = true;
    private boolean leftAlignment = false;
    private int numberOfTopPagesToSkipBeforeDelete = 0;
    private int numberOfTopTextLinesToDelete = 0;
    private int numberOfBottomTextLinesToDelete = 0;

    /**
     * 单次写入向量库的批大小。
     */
    private int writeBatchSize = 8;

    /**
     * 切片数量达到阈值后是否启用并行批量写入。
     */
    private boolean parallelWriteEnabled = false;

    /**
     * 启用并行批量写入的最小切片数阈值。
     */
    private int parallelWriteThreshold = 64;

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

    public int getWriteBatchSize() {
        return writeBatchSize;
    }

    public void setWriteBatchSize(int writeBatchSize) {
        this.writeBatchSize = writeBatchSize;
    }

    public boolean isParallelWriteEnabled() {
        return parallelWriteEnabled;
    }

    public void setParallelWriteEnabled(boolean parallelWriteEnabled) {
        this.parallelWriteEnabled = parallelWriteEnabled;
    }

    public int getParallelWriteThreshold() {
        return parallelWriteThreshold;
    }

    public void setParallelWriteThreshold(int parallelWriteThreshold) {
        this.parallelWriteThreshold = parallelWriteThreshold;
    }
}
