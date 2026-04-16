package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量文件切片列表响应")
public class VectorStoreDocumentListResponse {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "切片总数")
    private Integer total;

    @Schema(description = "切片列表")
    private List<VectorStoreDocumentResponse> items;
}
