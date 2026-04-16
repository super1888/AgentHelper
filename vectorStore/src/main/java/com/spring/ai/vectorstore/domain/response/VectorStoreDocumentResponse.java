package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量文档切片响应")
public class VectorStoreDocumentResponse {

    @Schema(description = "切片ID")
    private String id;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "相似度分值")
    private Double score;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;
}
