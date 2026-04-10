package com.spring.ai.vectorstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 向量检索响应结果。
 *
 * @param query 检索关键词
 * @param fileName 文件名过滤条件
 * @param topK 返回结果数量
 * @param similarityThreshold 相似度阈值
 * @param total 实际命中数量
 * @param items 命中文档列表
 */
@Schema(description = "向量检索响应结果")
public record VectorStoreSearchResponse(

        @Schema(description = "检索关键词")
        String query,

        @Schema(description = "文件名过滤条件")
        String fileName,

        @Schema(description = "返回结果数量")
        Integer topK,

        @Schema(description = "相似度阈值")
        Double similarityThreshold,

        @Schema(description = "实际命中数量")
        Integer total,

        @Schema(description = "命中文档列表")
        List<VectorStoreDocumentResponse> items
) {
}
