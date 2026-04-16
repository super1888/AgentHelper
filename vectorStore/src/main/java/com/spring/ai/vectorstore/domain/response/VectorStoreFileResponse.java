package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：向量存储文件汇总响应对象
 * 作者：Codex
 * 创建时间：2026-04-16
 * 核心功能：承载向量存储中按文件聚合后的管理信息，包括文件名、类型、大小、切片数量与上传时间。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量存储文件汇总响应对象")
public class VectorStoreFileResponse {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件后缀")
    private String fileExtension;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @Schema(description = "当前文件在向量库中的切片数量")
    private Integer chunkCount;

    @Schema(description = "最近一次上传时间")
    private String uploadedAt;
}
