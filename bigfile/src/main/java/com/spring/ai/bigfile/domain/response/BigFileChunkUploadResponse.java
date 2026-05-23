package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BigFileChunkUploadResponse {

    private String fileId;
    private Integer chunkIndex;
    private String chunkMd5;
    private String status;
    private Integer uploadedCount;
    private Integer totalChunks;
    private Boolean completed;
}
