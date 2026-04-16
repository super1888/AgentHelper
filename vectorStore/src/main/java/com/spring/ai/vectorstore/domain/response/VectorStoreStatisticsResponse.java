package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量管理统计响应")
public class VectorStoreStatisticsResponse {

    @Schema(description = "总文件数")
    private Integer totalFiles;

    @Schema(description = "启用中文件数")
    private Integer activeFiles;

    @Schema(description = "已删除文件数")
    private Integer deletedFiles;

    @Schema(description = "总切片数")
    private Integer totalChunks;

    @Schema(description = "总文件大小")
    private Long totalFileSize;
}
