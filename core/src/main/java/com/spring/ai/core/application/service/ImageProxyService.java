package com.spring.ai.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.core.domain.request.ImageGenerationProxyRequest;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    @Resource
    private ObjectMapper objectMapper;

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
            requestBody = objectMapper.writeValueAsString(request.getPayload());
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
