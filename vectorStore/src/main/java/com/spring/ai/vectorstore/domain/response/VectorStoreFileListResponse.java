package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：向量存储文件列表响应对象
 * 作者：Codex
 * 创建时间：2026-04-16
 * 核心功能：封装向量存储管理页所需的文件列表与总数信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量存储文件列表响应对象")
public class VectorStoreFileListResponse {

    @Schema(description = "文件总数")
    private Integer total;

    @Schema(description = "文件列表")
    private List<VectorStoreFileResponse> items;
}
