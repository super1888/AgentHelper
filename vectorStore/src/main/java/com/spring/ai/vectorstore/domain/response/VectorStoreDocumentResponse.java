package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量检索单条命中文档。
 * @author zhuoqi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量检索单条命中文档")
public class VectorStoreDocumentResponse {

    @Schema(description = "文档唯一标识")
    private String id;

    @Schema(description = "文档内容")
    private String content;

    @Schema(description = "相似度分值")
    private Double score;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;
}
