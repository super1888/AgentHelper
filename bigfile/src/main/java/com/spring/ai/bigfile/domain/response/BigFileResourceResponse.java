package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class BigFileResourceResponse {

    private String fileId;
    private String fileName;
    private String fileMd5;
    private String contentType;
    private String businessModule;
    private Long fileSize;
    private Path storagePath;
}