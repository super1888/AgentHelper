package com.spring.ai.vectorstore.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量删除响应结果。
 * @author zhuoqi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量删除响应结果")
public class VectorStoreDeleteResponse {

    @Schema(description = "操作类型")
    private String action;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "响应消息")
    private String message;
}
