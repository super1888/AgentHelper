package com.spring.ai.vectorstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 向量检索单条命中文档。
 *
 * @param id 文档唯一标识
 * @param content 文档内容
 * @param score 相似度分值
 * @param metadata 元数据
 */
@Schema(description = "向量检索单条命中文档")
public record VectorStoreDocumentResponse(

        @Schema(description = "文档唯一标识")
        String id,

        @Schema(description = "文档内容")
        String content,

        @Schema(description = "相似度分值")
        Double score,

        @Schema(description = "元数据")
        Map<String, Object> metadata
) {
}
