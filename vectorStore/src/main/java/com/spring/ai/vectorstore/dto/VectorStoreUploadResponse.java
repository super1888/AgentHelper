package com.spring.ai.vectorstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 向量库文件上传响应结果。
 *
 * @param fileName 文件名
 * @param fileExtension 文件后缀
 * @param sourceDocumentCount 源文档数量
 * @param chunkCount 切片数量
 * @param fileSize 文件大小（字节）
 * @param uploadedAt 上传时间
 * @param message 响应消息
 */
@Schema(description = "向量库文件上传响应结果")
public record VectorStoreUploadResponse(

        @Schema(description = "文件名")
        String fileName,

        @Schema(description = "文件后缀")
        String fileExtension,

        @Schema(description = "源文档数量")
        Integer sourceDocumentCount,

        @Schema(description = "切片数量")
        Integer chunkCount,

        @Schema(description = "文件大小（字节）")
        Long fileSize,

        @Schema(description = "上传时间")
        String uploadedAt,

        @Schema(description = "响应消息")
        String message
) {
}
