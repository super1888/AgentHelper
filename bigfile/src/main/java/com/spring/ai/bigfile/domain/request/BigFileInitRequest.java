package com.spring.ai.bigfile.domain.request;

import lombok.Data;

@Data
public class BigFileInitRequest {

    private String fileName;

    private Long fileSize;

    private Long chunkSize;

    private Integer totalChunks;
    private String fileMd5;
    private String contentType;
    private String businessModule;
}
