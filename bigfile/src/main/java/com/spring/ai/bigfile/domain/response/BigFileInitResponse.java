package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BigFileInitResponse {

    private String fileId;
    private String status;
    private Long chunkSize;
    private Integer totalChunks;
    private List<Integer> uploadedChunks;
    private String message;
}
