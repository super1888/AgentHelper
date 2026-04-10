package com.spring.ai.vectorstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 向量删除响应结果。
 *
 * @param action 操作类型
 * @param fileName 文件名
 * @param message 响应消息
 */
@Schema(description = "向量删除响应结果")
public record VectorStoreDeleteResponse(

        @Schema(description = "操作类型")
        String action,

        @Schema(description = "文件名")
        String fileName,

        @Schema(description = "响应消息")
        String message
) {
}
