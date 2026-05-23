package com.spring.ai.bigfile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.bigfile.config.BigFileProperties;
import com.spring.ai.bigfile.domain.dto.BigFileManifest;
import com.spring.ai.bigfile.domain.request.BigFileInitRequest;
import com.spring.ai.bigfile.domain.response.BigFileChunkUploadResponse;
import com.spring.ai.bigfile.domain.response.BigFileInitResponse;
import com.spring.ai.bigfile.domain.response.BigFileListResponse;
import com.spring.ai.bigfile.domain.response.BigFileMergeResponse;
import com.spring.ai.bigfile.domain.response.BigFileMissingChunksResponse;
import com.spring.ai.bigfile.domain.response.BigFileRecordResponse;
import com.spring.ai.bigfile.domain.response.BigFileResourceResponse;
import com.spring.ai.bigfile.domain.response.BigFileStatisticsResponse;
import com.spring.ai.bigfile.service.BigFileService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class BigFileServiceImpl implements BigFileService {

    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Resource
    private BigFileProperties bigFileProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Path rootPath;
    private Path recordsPath;

    @PostConstruct
    public void initStorage() throws IOException {
        rootPath = Path.of(bigFileProperties.getStorageRoot()).toAbsolutePath().normalize();
        recordsPath = rootPath.resolve("records");
        Files.createDirectories(recordsPath);
    }

    @Override
    public BigFileInitResponse init(BigFileInitRequest request) {
        validateInitRequest(request);
        BigFileManifest existed = findCompletedByMd5(request.getFileMd5());
        if (existed != null) {
            return BigFileInitResponse.builder()
                    .fileId(existed.getFileId())
                    .status(existed.getStatus())
                    .chunkSize(existed.getChunkSize())
                    .totalChunks(existed.getTotalChunks())
                    .uploadedChunks(sortedChunks(existed))
                    .message("文件已上传，可直接复用")
                    .build();
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        String now = now();
        BigFileManifest manifest = new BigFileManifest();
        manifest.setFileId(fileId);
        manifest.setFileName(cleanFileName(request.getFileName()));
        manifest.setFileMd5(resolveFileFingerprint(request));
        manifest.setContentType(trimToDefault(request.getContentType(), "application/octet-stream"));
        manifest.setBusinessModule(trimToDefault(request.getBusinessModule(), "vectorStore"));
        manifest.setFileSize(request.getFileSize());
        manifest.setChunkSize(resolveChunkSize(request));
        manifest.setTotalChunks(resolveTotalChunks(request));
        manifest.setUploadedChunks(new ArrayList<>());
        manifest.setStatus(STATUS_UPLOADING);
        manifest.setCreatedAt(now);
        manifest.setUpdatedAt(now);
        manifest.setLastMessage("文件上传任务已创建");
        saveManifest(manifest);

        return BigFileInitResponse.builder()
                .fileId(fileId)
                .status(manifest.getStatus())
                .chunkSize(manifest.getChunkSize())
                .totalChunks(manifest.getTotalChunks())
                .uploadedChunks(List.of())
                .message(manifest.getLastMessage())
                .build();
    }

    @Override
    public BigFileChunkUploadResponse uploadChunk(String fileId, Integer chunkIndex, String chunkMd5, MultipartFile chunk) {
        BigFileManifest manifest = loadManifest(fileId);
        if (STATUS_COMPLETED.equals(manifest.getStatus())) {
            return chunkResponse(manifest, chunkIndex, chunkMd5);
        }
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= manifest.getTotalChunks()) {
            throw new IllegalArgumentException("分片序号超出范围");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new IllegalArgumentException("分片文件不能为空");
        }

        try {
            Path chunksPath = chunksPath(fileId);
            Files.createDirectories(chunksPath);
            Path chunkPath = chunksPath.resolve(chunkIndex + ".part");
            Files.copy(chunk.getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
            String actualMd5 = md5(chunkPath);
            if (StringUtils.hasText(chunkMd5) && !actualMd5.equalsIgnoreCase(chunkMd5.trim())) {
                Files.deleteIfExists(chunkPath);
                throw new IllegalArgumentException("分片 MD5 校验失败");
            }
            Set<Integer> uploadedChunks = new LinkedHashSet<>(manifest.getUploadedChunks());
            uploadedChunks.add(chunkIndex);
            manifest.setUploadedChunks(new ArrayList<>(uploadedChunks));
            manifest.setStatus(STATUS_UPLOADING);
            manifest.setUpdatedAt(now());
            manifest.setLastMessage("分片上传成功");
            saveManifest(manifest);
            return chunkResponse(manifest, chunkIndex, actualMd5);
        } catch (IOException exception) {
            markFailed(manifest, "分片保存失败：" + exception.getMessage());
            throw new IllegalStateException("分片保存失败", exception);
        }
    }

    @Override
    public BigFileMissingChunksResponse missingChunks(String fileId) {
        BigFileManifest manifest = loadManifest(fileId);
        List<Integer> uploadedChunks = sortedChunks(manifest);
        List<Integer> missingChunks = new ArrayList<>();
        for (int index = 0; index < manifest.getTotalChunks(); index++) {
            if (!uploadedChunks.contains(index)) {
                missingChunks.add(index);
            }
        }
        return BigFileMissingChunksResponse.builder()
                .fileId(manifest.getFileId())
                .uploadedChunks(uploadedChunks)
                .missingChunks(missingChunks)
                .uploadedCount(uploadedChunks.size())
                .totalChunks(manifest.getTotalChunks())
                .status(manifest.getStatus())
                .build();
    }

    @Override
    public BigFileMergeResponse merge(String fileId) {
        BigFileManifest manifest = loadManifest(fileId);
        BigFileMissingChunksResponse chunks = missingChunks(fileId);
        if (!chunks.getMissingChunks().isEmpty()) {
            throw new IllegalStateException("仍有分片未上传完成");
        }
        try {
            Path completedPath = completedPath(fileId, manifest.getFileName());
            Files.createDirectories(completedPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(completedPath)) {
                for (int index = 0; index < manifest.getTotalChunks(); index++) {
                    Files.copy(chunksPath(fileId).resolve(index + ".part"), outputStream);
                }
            }
            String actualMd5 = md5(completedPath);
            if (isMd5(manifest.getFileMd5()) && !actualMd5.equalsIgnoreCase(manifest.getFileMd5())) {
                Files.deleteIfExists(completedPath);
                markFailed(manifest, "合并后 MD5 校验失败");
                throw new IllegalArgumentException("合并后 MD5 校验失败");
            }
            manifest.setStatus(STATUS_COMPLETED);
            manifest.setStoragePath(completedPath.toString());
            manifest.setUpdatedAt(now());
            manifest.setLastMessage("文件已合并完成");
            saveManifest(manifest);
            return BigFileMergeResponse.builder()
                    .fileId(manifest.getFileId())
                    .fileName(manifest.getFileName())
                    .fileSize(manifest.getFileSize())
                    .fileMd5(actualMd5)
                    .storagePath(manifest.getStoragePath())
                    .status(manifest.getStatus())
                    .message(manifest.getLastMessage())
                    .build();
        } catch (IOException exception) {
            markFailed(manifest, "文件合并失败：" + exception.getMessage());
            throw new IllegalStateException("文件合并失败", exception);
        }
    }

    @Override
    public BigFileListResponse list(String keyword, String status, String businessModule) {
        List<BigFileRecordResponse> items = listManifests().stream()
                .filter(item -> matches(keyword, item.getFileName()) || matches(keyword, item.getFileMd5()))
                .filter(item -> !StringUtils.hasText(status) || status.trim().equalsIgnoreCase(item.getStatus()))
                .filter(item -> !StringUtils.hasText(businessModule) || businessModule.trim().equalsIgnoreCase(item.getBusinessModule()))
                .sorted(Comparator.comparing(BigFileManifest::getUpdatedAt, Comparator.nullsLast(String::compareTo)).reversed())
                .map(this::toRecordResponse)
                .toList();
        return BigFileListResponse.builder().items(items).build();
    }

    @Override
    public BigFileStatisticsResponse statistics() {
        List<BigFileManifest> manifests = listManifests();
        long totalSize = manifests.stream()
                .filter(item -> STATUS_COMPLETED.equals(item.getStatus()))
                .map(BigFileManifest::getFileSize)
                .filter(size -> size != null)
                .mapToLong(Long::longValue)
                .sum();
        return BigFileStatisticsResponse.builder()
                .totalFiles(manifests.size())
                .completedFiles((int) manifests.stream().filter(item -> STATUS_COMPLETED.equals(item.getStatus())).count())
                .uploadingFiles((int) manifests.stream().filter(item -> STATUS_UPLOADING.equals(item.getStatus())).count())
                .failedFiles((int) manifests.stream().filter(item -> STATUS_FAILED.equals(item.getStatus())).count())
                .totalFileSize(totalSize)
                .maxFileSize(bigFileProperties.getMaxFileSize())
                .defaultChunkSize(bigFileProperties.getDefaultChunkSize())
                .build();
    }

    @Override
    public BigFileResourceResponse getCompletedFile(String fileId) {
        BigFileManifest manifest = loadManifest(fileId);
        if (!STATUS_COMPLETED.equals(manifest.getStatus())) {
            throw new IllegalStateException("文件尚未合并完成，不能用于向量入库");
        }
        if (!StringUtils.hasText(manifest.getStoragePath())) {
            throw new IllegalStateException("文件存储路径不存在");
        }
        Path storagePath = Path.of(manifest.getStoragePath()).toAbsolutePath().normalize();
        if (!Files.exists(storagePath) || !Files.isRegularFile(storagePath)) {
            throw new IllegalStateException("合并后的文件不存在");
        }
        return BigFileResourceResponse.builder()
                .fileId(manifest.getFileId())
                .fileName(manifest.getFileName())
                .fileMd5(manifest.getFileMd5())
                .contentType(manifest.getContentType())
                .businessModule(manifest.getBusinessModule())
                .fileSize(manifest.getFileSize())
                .storagePath(storagePath)
                .build();
    }
    @Override
    public void delete(String fileId) {
        try {
            FileSystemUtils.deleteRecursively(recordPath(fileId));
        } catch (IOException exception) {
            throw new IllegalStateException("文件记录删除失败", exception);
        }
    }

    private void validateInitRequest(BigFileInitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("上传初始化参数不能为空");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new IllegalArgumentException("文件大小必须大于 0");
        }
        if (request.getFileSize() > bigFileProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过系统限制");
        }
    }

    private BigFileChunkUploadResponse chunkResponse(BigFileManifest manifest, Integer chunkIndex, String chunkMd5) {
        int uploadedCount = manifest.getUploadedChunks() == null ? 0 : manifest.getUploadedChunks().size();
        return BigFileChunkUploadResponse.builder()
                .fileId(manifest.getFileId())
                .chunkIndex(chunkIndex)
                .chunkMd5(chunkMd5)
                .status(manifest.getStatus())
                .uploadedCount(uploadedCount)
                .totalChunks(manifest.getTotalChunks())
                .completed(uploadedCount == manifest.getTotalChunks())
                .build();
    }

    private BigFileRecordResponse toRecordResponse(BigFileManifest manifest) {
        return BigFileRecordResponse.builder()
                .fileId(manifest.getFileId())
                .fileName(manifest.getFileName())
                .fileMd5(manifest.getFileMd5())
                .contentType(manifest.getContentType())
                .businessModule(manifest.getBusinessModule())
                .fileSize(manifest.getFileSize())
                .chunkSize(manifest.getChunkSize())
                .totalChunks(manifest.getTotalChunks())
                .uploadedCount(manifest.getUploadedChunks() == null ? 0 : manifest.getUploadedChunks().size())
                .status(manifest.getStatus())
                .storagePath(manifest.getStoragePath())
                .createdAt(manifest.getCreatedAt())
                .updatedAt(manifest.getUpdatedAt())
                .lastMessage(manifest.getLastMessage())
                .build();
    }

    private List<BigFileManifest> listManifests() {
        try (Stream<Path> paths = Files.list(recordsPath)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.resolve(MANIFEST_FILE))
                    .filter(Files::exists)
                    .map(this::readManifest)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private BigFileManifest findCompletedByMd5(String fileMd5) {
        if (!StringUtils.hasText(fileMd5)) {
            return null;
        }
        return listManifests().stream()
                .filter(item -> STATUS_COMPLETED.equals(item.getStatus()))
                .filter(item -> fileMd5.trim().equalsIgnoreCase(item.getFileMd5()))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String keyword, String value) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private List<Integer> sortedChunks(BigFileManifest manifest) {
        if (manifest.getUploadedChunks() == null) {
            return List.of();
        }
        return manifest.getUploadedChunks().stream().distinct().sorted().toList();
    }

    private long resolveChunkSize(BigFileInitRequest request) {
        if (request.getChunkSize() != null && request.getChunkSize() > 0) {
            return request.getChunkSize();
        }
        return bigFileProperties.getDefaultChunkSize();
    }

    private int resolveTotalChunks(BigFileInitRequest request) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 0) {
            return request.getTotalChunks();
        }
        return (int) Math.ceil((double) request.getFileSize() / resolveChunkSize(request));
    }

    private void saveManifest(BigFileManifest manifest) {
        try {
            Files.createDirectories(recordPath(manifest.getFileId()));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath(manifest.getFileId()).toFile(), manifest);
        } catch (IOException exception) {
            throw new IllegalStateException("上传元数据保存失败", exception);
        }
    }

    private BigFileManifest loadManifest(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            throw new IllegalArgumentException("文件唯一标识不能为空");
        }
        Path manifestPath = manifestPath(fileId);
        if (!Files.exists(manifestPath)) {
            throw new IllegalArgumentException("文件上传记录不存在");
        }
        return readManifest(manifestPath);
    }

    private BigFileManifest readManifest(Path manifestPath) {
        try {
            return objectMapper.readValue(manifestPath.toFile(), BigFileManifest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("上传元数据读取失败", exception);
        }
    }

    private void markFailed(BigFileManifest manifest, String message) {
        manifest.setStatus(STATUS_FAILED);
        manifest.setUpdatedAt(now());
        manifest.setLastMessage(message);
        saveManifest(manifest);
    }

    private Path recordPath(String fileId) {
        return recordsPath.resolve(fileId).normalize();
    }

    private Path manifestPath(String fileId) {
        return recordPath(fileId).resolve(MANIFEST_FILE);
    }

    private Path chunksPath(String fileId) {
        return recordPath(fileId).resolve("chunks");
    }

    private Path completedPath(String fileId, String fileName) {
        return recordPath(fileId).resolve("completed").resolve(fileName);
    }

    private String md5(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return DigestUtils.md5DigestAsHex(inputStream);
        }
    }

    private String cleanFileName(String fileName) {
        String cleaned = Path.of(fileName).getFileName().toString().trim();
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return cleaned;
    }

    private String resolveFileFingerprint(BigFileInitRequest request) {
        if (StringUtils.hasText(request.getFileMd5())) {
            return request.getFileMd5().trim().toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isMd5(String value) {
        return StringUtils.hasText(value) && value.matches("^[a-fA-F0-9]{32}$");
    }

    private String trimToDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}

