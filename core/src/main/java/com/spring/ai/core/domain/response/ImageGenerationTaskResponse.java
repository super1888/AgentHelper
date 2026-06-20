package com.spring.ai.core.domain.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 图片异步任务响应。
 */
@Data
@Builder
public class ImageGenerationTaskResponse {

    private String taskId;

    private String status;

    private String message;

    private String createTime;

    private String updateTime;

    private Long durationMillis;

    @Builder.Default
    private List<ImageGenerationTaskResult> results = new ArrayList<>();

    private String rawResponse;

    @Data
    @Builder
    public static class ImageGenerationTaskResult {

        private String fileName;

        private String filePath;

        private String downloadUrl;

        private String mimeType;

        private Long fileSize;
    }
}