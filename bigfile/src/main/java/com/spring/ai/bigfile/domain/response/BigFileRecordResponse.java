package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BigFileRecordResponse {

    private String fileId;
    private String fileName;
    private String fileMd5;
    private String contentType;
    private String businessModule;
    private Long fileSize;
    private Long chunkSize;
    private Integer totalChunks;
    private Integer uploadedCount;
    private String status;
    private String storagePath;
    private String createdAt;
    private String updatedAt;
    private String lastMessage;
}
