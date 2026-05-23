package com.spring.ai.bigfile.service;

import com.spring.ai.bigfile.domain.request.BigFileInitRequest;
import com.spring.ai.bigfile.domain.response.BigFileChunkUploadResponse;
import com.spring.ai.bigfile.domain.response.BigFileInitResponse;
import com.spring.ai.bigfile.domain.response.BigFileListResponse;
import com.spring.ai.bigfile.domain.response.BigFileMergeResponse;
import com.spring.ai.bigfile.domain.response.BigFileMissingChunksResponse;
import com.spring.ai.bigfile.domain.response.BigFileResourceResponse;
import com.spring.ai.bigfile.domain.response.BigFileStatisticsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BigFileService {

    BigFileInitResponse init(BigFileInitRequest request);

    BigFileChunkUploadResponse uploadChunk(String fileId, Integer chunkIndex, String chunkMd5, MultipartFile chunk);

    BigFileMissingChunksResponse missingChunks(String fileId);

    BigFileMergeResponse merge(String fileId);

    BigFileListResponse list(String keyword, String status, String businessModule);

    BigFileStatisticsResponse statistics();

    BigFileResourceResponse getCompletedFile(String fileId);

    void delete(String fileId);
}
