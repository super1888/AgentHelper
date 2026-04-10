package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量检索响应结果。
 * @author zhuoqi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量检索响应结果")
public class VectorStoreSearchResponse {

    @Schema(description = "检索关键词")
    private String query;

    @Schema(description = "文件名过滤条件")
    private String fileName;

    @Schema(description = "返回结果数量")
    private Integer topK;

    @Schema(description = "相似度阈值")
    private Double similarityThreshold;

    @Schema(description = "实际命中数量")
    private Integer total;

    @Schema(description = "命中文档列表")
    private List<VectorStoreDocumentResponse> items;
}
