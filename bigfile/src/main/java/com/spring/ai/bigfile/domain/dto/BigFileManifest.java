package com.spring.ai.bigfile.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BigFileManifest {

    private String fileId;
    private String fileName;
    private String fileMd5;
    private String contentType;
    private String businessModule;
    private Long fileSize;
    private Long chunkSize;
    private Integer totalChunks;
    private List<Integer> uploadedChunks = new ArrayList<>();
    private String status;
    private String storagePath;
    private String createdAt;
    private String updatedAt;
    private String lastMessage;
}
