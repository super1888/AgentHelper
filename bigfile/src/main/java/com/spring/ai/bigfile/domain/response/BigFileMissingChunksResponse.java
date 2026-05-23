package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BigFileMissingChunksResponse {

    private String fileId;
    private List<Integer> uploadedChunks;
    private List<Integer> missingChunks;
    private Integer uploadedCount;
    private Integer totalChunks;
    private String status;
}
