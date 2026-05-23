package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BigFileStatisticsResponse {

    private Integer totalFiles;
    private Integer completedFiles;
    private Integer uploadingFiles;
    private Integer failedFiles;
    private Long totalFileSize;
    private Long maxFileSize;
    private Long defaultChunkSize;
}
