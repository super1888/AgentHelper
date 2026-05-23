package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BigFileMergeResponse {

    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileMd5;
    private String storagePath;
    private String status;
    private String message;
}
