package com.spring.ai.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.core.domain.request.ImageGenerationProxyRequest;
import com.spring.ai.core.domain.response.ImageGenerationTaskResponse;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
/**
 * 图片接口代理服务。
 * 负责将浏览器同源请求转发到上游图片生成与编辑接口，规避浏览器 CORS 限制。
 */
@Component
public class ImageProxyService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_SUCCESS = "SUCCESS";
    private static final String TASK_FAILED = "FAILED";

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    @Qualifier(CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;

    @Value("${app.image.storage-root:${user.dir}/data/images}")
    private String imageStorageRoot;

    private final Map<String, ImageGenerationTaskResponse> imageTasks = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * 转发图片生成请求。
     */
    public ResponseEntity<String> proxyGeneration(ImageGenerationProxyRequest request) {
        validateGenerationRequest(request);
        String targetUrl = buildTargetUrl(request.getBaseUrl(), request.getEndpointPath(), "/v1/images/generations");

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(normalizeGenerationPayload(request.getPayload()));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片生成载荷序列化失败");
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + request.getApiKey().trim())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        return send(httpRequest);
    }

    /**
     * 提交异步图片生成任务，避免前端等待上游长时间连接导致超时。
     */
    public ImageGenerationTaskResponse submitGenerationTask(ImageGenerationProxyRequest request) {
        validateGenerationRequest(request);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImageGenerationTaskResponse task = ImageGenerationTaskResponse.builder()
                .taskId(taskId)
                .status(TASK_PENDING)
                .message("图片生成任务已提交")
                .createTime(now())
                .updateTime(now())
                .durationMillis(0L)
                .results(new ArrayList<>())
                .build();
        imageTasks.put(taskId, task);
        CompletableFuture.runAsync(() -> runGenerationTask(taskId, request), commonAsyncExecutor);
        return task;
    }

    public ImageGenerationTaskResponse getGenerationTask(String taskId) {
        ImageGenerationTaskResponse task = imageTasks.get(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "图片生成任务不存在");
        }
        return task;
    }

    public ResponseEntity<byte[]> readGeneratedImage(String taskId, String fileName) {
        ImageGenerationTaskResponse task = getGenerationTask(taskId);
        ImageGenerationTaskResponse.ImageGenerationTaskResult result = task.getResults().stream()
                .filter(item -> fileName.equals(item.getFileName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "图片文件不存在"));
        try {
            byte[] bytes = Files.readAllBytes(Path.of(result.getFilePath()).toAbsolutePath().normalize());
            return ResponseEntity.ok()
                    .contentType(parseMediaType(result.getMimeType()))
                    .body(bytes);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "读取图片文件失败", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void runGenerationTask(String taskId, ImageGenerationProxyRequest request) {
        long start = System.currentTimeMillis();
        updateTask(taskId, TASK_RUNNING, "图片生成中，请稍后刷新任务状态", null, null, null);
        try {
            String targetUrl = buildTargetUrl(request.getBaseUrl(), request.getEndpointPath(), "/v1/images/generations");
            Map<String, Object> payload = normalizeGenerationPayload(request.getPayload());
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + request.getApiKey().trim())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                updateTask(taskId, TASK_FAILED, "上游返回失败：HTTP " + response.statusCode(), null, response.body(), elapsed(start));
                return;
            }
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<ImageGenerationTaskResponse.ImageGenerationTaskResult> results = saveGeneratedImages(taskId, responseMap, payload);
            updateTask(taskId, TASK_SUCCESS, "图片生成完成，共保存 " + results.size() + " 张图片", results, response.body(), elapsed(start));
        } catch (Exception exception) {
            updateTask(taskId, TASK_FAILED, "图片生成失败：" + exception.getMessage(), null, null, elapsed(start));
        }
    }

    @SuppressWarnings("unchecked")
    private List<ImageGenerationTaskResponse.ImageGenerationTaskResult> saveGeneratedImages(String taskId,
                                                                                            Map<String, Object> responseMap,
                                                                                            Map<String, Object> payload) throws IOException {
        Object dataValue = responseMap.get("data");
        if (!(dataValue instanceof List<?> dataList)) {
            return Collections.emptyList();
        }
        Path taskDirectory = Path.of(imageStorageRoot).toAbsolutePath().normalize().resolve(taskId);
        Files.createDirectories(taskDirectory);
        String outputFormat = String.valueOf(payload.getOrDefault("output_format", "png")).toLowerCase();
        String extension = normalizeImageExtension(outputFormat);
        String mimeType = normalizeImageMimeType(extension);
        List<ImageGenerationTaskResponse.ImageGenerationTaskResult> results = new ArrayList<>();
        int index = 1;
        for (Object item : dataList) {
            if (!(item instanceof Map<?, ?> rawItem)) {
                continue;
            }
            byte[] imageBytes = null;
            try {
                imageBytes = resolveImageBytes(rawItem);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (imageBytes.length == 0) {
                continue;
            }
            String fileName = "image-" + index + "." + extension;
            Path filePath = taskDirectory.resolve(fileName).normalize();
            Files.write(filePath, imageBytes);
            results.add(ImageGenerationTaskResponse.ImageGenerationTaskResult.builder()
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .downloadUrl("/agentHelper/core/image-proxy/generation-tasks/" + taskId + "/files/" + fileName)
                    .mimeType(mimeType)
                    .fileSize((long) imageBytes.length)
                    .build());
            index++;
        }
        return results;
    }

    private byte[] resolveImageBytes(Map<?, ?> rawItem) throws IOException, InterruptedException {
        Object base64Value = rawItem.get("b64_json");
        if (base64Value instanceof String base64Text && StringUtils.hasText(base64Text)) {
            return Base64.getDecoder().decode(base64Text.trim());
        }
        Object urlValue = rawItem.get("url");
        if (urlValue instanceof String url && StringUtils.hasText(url)) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.trim()))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() >= 200 && response.statusCode() < 300 ? response.body() : new byte[0];
        }
        return new byte[0];
    }

    private void updateTask(String taskId,
                            String status,
                            String message,
                            List<ImageGenerationTaskResponse.ImageGenerationTaskResult> results,
                            String rawResponse,
                            Long durationMillis) {
        ImageGenerationTaskResponse task = imageTasks.get(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setMessage(message);
        task.setUpdateTime(now());
        if (results != null) {
            task.setResults(results);
        }
        if (rawResponse != null) {
            task.setRawResponse(rawResponse);
        }
        if (durationMillis != null) {
            task.setDurationMillis(durationMillis);
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private String normalizeImageExtension(String outputFormat) {
        if ("jpeg".equals(outputFormat) || "jpg".equals(outputFormat)) {
            return "jpg";
        }
        if ("webp".equals(outputFormat)) {
            return "webp";
        }
        return "png";
    }

    private String normalizeImageMimeType(String extension) {
        if ("jpg".equals(extension)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if ("webp".equals(extension)) {
            return "image/webp";
        }
        return MediaType.IMAGE_PNG_VALUE;
    }

    /**
     * 转发图片编辑请求。
     */
    public ResponseEntity<String> proxyEdit(String baseUrl,
                                            String apiKey,
                                            String endpointPath,
                                            Map<String, String> formFields,
                                            MultipartFile[] imageFiles,
                                            MultipartFile maskFile) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "SUB2API_BASE 不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "SUB2API_KEY 不能为空");
        }
        if (imageFiles == null || imageFiles.length == 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "至少需要上传一张主图");
        }

        String targetUrl = buildTargetUrl(baseUrl, endpointPath, "/v1/images/edits");
        String boundary = "----SpringAiImageProxy" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildMultipartBody(boundary, formFields, imageFiles, maskFile);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        return send(httpRequest);
    }

    private void validateGenerationRequest(ImageGenerationProxyRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片生成请求不能为空");
        }
        if (!StringUtils.hasText(request.getBaseUrl())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "SUB2API_BASE 不能为空");
        }
        if (!StringUtils.hasText(request.getApiKey())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "SUB2API_KEY 不能为空");
        }
        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片生成载荷不能为空");
        }
    }

    /**
     * 规范化图片生成请求体，确保 size、n、quality 等参数按上游接口要求透传。
     */
    private Map<String, Object> normalizeGenerationPayload(Map<String, Object> rawPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        rawPayload.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || value == null) {
                return;
            }
            if (value instanceof String text && !StringUtils.hasText(text)) {
                return;
            }
            payload.put(key, value);
        });
        normalizeIntegerField(payload, "n", 1, 10);
        normalizeIntegerField(payload, "output_compression", 0, 100);
        normalizeStringField(payload, "model");
        normalizeStringField(payload, "prompt");
        normalizeStringField(payload, "size");
        normalizeStringField(payload, "quality");
        normalizeStringField(payload, "background");
        normalizeStringField(payload, "moderation");
        normalizeStringField(payload, "output_format");
        normalizeStringField(payload, "user");
        return payload;
    }

    private void normalizeStringField(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (StringUtils.hasText(trimmed)) {
                payload.put(fieldName, trimmed);
            } else {
                payload.remove(fieldName);
            }
        }
    }

    private void normalizeIntegerField(Map<String, Object> payload, String fieldName, int min, int max) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return;
        }
        try {
            int number = value instanceof Number numericValue
                    ? numericValue.intValue()
                    : Integer.parseInt(String.valueOf(value).trim());
            payload.put(fieldName, Math.max(min, Math.min(max, number)));
        } catch (RuntimeException exception) {
            payload.remove(fieldName);
        }
    }

    private String buildTargetUrl(String baseUrl, String endpointPath, String defaultPath) {
        String normalizedBaseUrl = baseUrl.trim().replaceAll("/+$", "");
        String normalizedPath = StringUtils.hasText(endpointPath) ? endpointPath.trim() : defaultPath;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedBaseUrl + normalizedPath;
    }

    private ResponseEntity<String> send(HttpRequest httpRequest) {
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            MediaType mediaType = parseMediaType(response.headers().firstValue("Content-Type").orElse(MediaType.APPLICATION_JSON_VALUE));
            return ResponseEntity.status(response.statusCode())
                    .contentType(mediaType)
                    .body(response.body());
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":{\"message\":\"" + escapeJson(exception.getMessage()) + "\",\"type\":\"upstream_error\"}}");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":{\"message\":\"" + escapeJson(exception.getMessage()) + "\",\"type\":\"upstream_error\"}}");
        }
    }

    /**
     * 手工组装 multipart/form-data，避免不同 HTTP 客户端在网关场景下的兼容差异。
     */
    private byte[] buildMultipartBody(String boundary,
                                      Map<String, String> rawFormFields,
                                      MultipartFile[] imageFiles,
                                      MultipartFile maskFile) {
        try {
            List<byte[]> chunks = new ArrayList<>();
            Map<String, String> formFields = new LinkedHashMap<>(rawFormFields);
            formFields.remove("baseUrl");
            formFields.remove("apiKey");
            formFields.remove("endpointPath");

            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                if (!StringUtils.hasText(entry.getValue())) {
                    continue;
                }
                chunks.add(textPart(boundary, entry.getKey(), entry.getValue()));
            }

            for (MultipartFile imageFile : imageFiles) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    chunks.add(filePart(boundary, "image[]", imageFile));
                }
            }

            if (maskFile != null && !maskFile.isEmpty()) {
                chunks.add(filePart(boundary, "mask", maskFile));
            }

            chunks.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return mergeChunks(chunks);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "读取上传文件失败", exception);
        }
    }

    private byte[] textPart(String boundary, String fieldName, String value) {
        String content = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n"
                + value + "\r\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] filePart(String boundary, String fieldName, MultipartFile file) throws IOException {
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + escapeQuotes(fileName) + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] footer = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = file.getBytes();

        byte[] result = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(fileBytes, 0, result, header.length, fileBytes.length);
        System.arraycopy(footer, 0, result, header.length + fileBytes.length, footer.length);
        return result;
    }

    private byte[] mergeChunks(List<byte[]> chunks) {
        int totalLength = chunks.stream().mapToInt(chunk -> chunk.length).sum();
        byte[] merged = new byte[totalLength];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, merged, offset, chunk.length);
            offset += chunk.length;
        }
        return merged;
    }

    private MediaType parseMediaType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (Exception exception) {
            return MediaType.APPLICATION_JSON;
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "Upstream request failed";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeQuotes(String value) {
        return value.replace("\"", "%22");
    }
}
