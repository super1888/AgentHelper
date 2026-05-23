package com.spring.ai.bigfile.controller;

import com.spring.ai.bigfile.domain.request.BigFileInitRequest;
import com.spring.ai.bigfile.domain.response.BigFileChunkUploadResponse;
import com.spring.ai.bigfile.domain.response.BigFileInitResponse;
import com.spring.ai.bigfile.domain.response.BigFileListResponse;
import com.spring.ai.bigfile.domain.response.BigFileMergeResponse;
import com.spring.ai.bigfile.domain.response.BigFileMissingChunksResponse;
import com.spring.ai.bigfile.domain.response.BigFileStatisticsResponse;
import com.spring.ai.bigfile.service.BigFileService;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/big-files")
public class BigFileController {

    @Resource
    private BigFileService bigFileService;

    @PostMapping("/init")
    public ApiResponse<BigFileInitResponse> init(@RequestBody BigFileInitRequest request) {
        return ApiResponse.success(bigFileService.init(request));
    }

    @PostMapping("/{fileId}/chunks")
    public ApiResponse<BigFileChunkUploadResponse> uploadChunk(@PathVariable String fileId,
                                                               @RequestParam Integer chunkIndex,
                                                               @RequestParam(required = false) String chunkMd5,
                                                               @RequestParam("chunk") MultipartFile chunk) {
        return ApiResponse.success(bigFileService.uploadChunk(fileId, chunkIndex, chunkMd5, chunk));
    }

    @GetMapping("/{fileId}/missing-chunks")
    public ApiResponse<BigFileMissingChunksResponse> missingChunks(@PathVariable String fileId) {
        return ApiResponse.success(bigFileService.missingChunks(fileId));
    }

    @PostMapping("/{fileId}/merge")
    public ApiResponse<BigFileMergeResponse> merge(@PathVariable String fileId) {
        return ApiResponse.success(bigFileService.merge(fileId));
    }

    @GetMapping
    public ApiResponse<BigFileListResponse> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String businessModule) {
        return ApiResponse.success(bigFileService.list(keyword, status, businessModule));
    }

    @GetMapping("/statistics")
    public ApiResponse<BigFileStatisticsResponse> statistics() {
        return ApiResponse.success(bigFileService.statistics());
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable String fileId) {
        bigFileService.delete(fileId);
        return ApiResponse.success(null);
    }
}
