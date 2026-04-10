package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量库文件上传响应结果。
 * @author zhuoqi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量库文件上传响应结果")
public class VectorStoreUploadResponse {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件后缀")
    private String fileExtension;

    @Schema(description = "源文档数量")
    private Integer sourceDocumentCount;

    @Schema(description = "切片数量")
    private Integer chunkCount;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "上传时间")
    private String uploadedAt;

    @Schema(description = "响应消息")
    private String message;
}
