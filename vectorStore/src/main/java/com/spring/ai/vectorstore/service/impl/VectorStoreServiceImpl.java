package com.spring.ai.vectorstore.service.impl;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_CONTENT_TYPE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_EXTENSION;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_SIZE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_SOURCE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_UPLOADED_AT;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.common.config.CommonAsyncConfig;
import com.spring.ai.common.utils.ParallelExecutionUtils;
import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import com.spring.ai.vectorstore.reader.MultipartDocumentReader;
import com.spring.ai.vectorstore.reader.MultipartDocumentReaderRegistry;
import com.spring.ai.vectorstore.service.VectorStoreService;
import com.spring.ai.vectorstore.store.RedisVectorStoreCapabilityChecker;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 向量库服务实现类。 负责串联文档读取、元数据补齐、文本切片、向量写入、检索与删除流程。
 */
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final int MIN_ADAPTIVE_CHUNK_SIZE = 100;
    private static final int MAX_ADAPTIVE_SPLIT_DEPTH = 3;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private VectorStoreProperties vectorStoreProperties;

    @Resource
    private MultipartDocumentReaderRegistry readerRegistry;

    @Resource
    private RedisVectorStoreCapabilityChecker capabilityChecker;

    @Resource(name = CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;

    @Override
    public VectorStoreUploadResponse upload(MultipartFile file) {
        validateFile(file);
        capabilityChecker.ensureReady();

        String fileName = requireFileName(file);
        String extension = resolveExtension(fileName);
        String uploadedAt = Instant.now().toString();

        MultipartDocumentReader reader = readerRegistry.getReader(extension);
        List<Document> sourceDocuments = reader.read(file);
        if (sourceDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("Document parsing returned no content");
        }

        List<Document> normalizedDocuments = sourceDocuments.stream()
                .map(document -> enrichDocument(document, file, fileName, extension, uploadedAt))
                .filter(this::hasTextContent)
                .toList();
        if (normalizedDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("Document content is blank after normalization");
        }

        List<Document> chunkDocuments = tokenTextSplitter.apply(normalizedDocuments);
        if (chunkDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("Document splitting returned no chunks");
        }

        persistDocuments(chunkDocuments);
        log.info("Stored vector document, fileName={}, sourceDocuments={}, chunks={}",
                fileName, sourceDocuments.size(), chunkDocuments.size());

        return VectorStoreUploadResponse.builder()
                .fileName(fileName)
                .fileExtension(extension)
                .sourceDocumentCount(sourceDocuments.size())
                .chunkCount(chunkDocuments.size())
                .fileSize(file.getSize())
                .uploadedAt(uploadedAt)
                .message("Document parsed, chunked and stored successfully")
                .build();
    }

    @Override
    public VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold) {
        capabilityChecker.ensureReady();

        String normalizedQuery = normalizeRequiredText(query, "Query must not be blank");
        String normalizedFileName = normalizeOptionalText(fileName);
        int validatedTopK = validateTopK(topK);
        Double validatedThreshold = validateSimilarityThreshold(similarityThreshold);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(normalizedQuery)
                .topK(validatedTopK)
                .filterExpression(buildSearchFilter(normalizedFileName));

        if (validatedThreshold != null) {
            builder.similarityThreshold(validatedThreshold);
        }

        try {
            List<VectorStoreDocumentResponse> items = vectorStore.similaritySearch(builder.build()).stream()
                    .map(this::toDocumentResponse)
                    .toList();

            return VectorStoreSearchResponse.builder()
                    .query(normalizedQuery)
                    .fileName(normalizedFileName)
                    .topK(validatedTopK)
                    .similarityThreshold(validatedThreshold)
                    .total(items.size())
                    .items(items)
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("search", exception);
        }
    }

    @Override
    public VectorStoreDeleteResponse deleteAll() {
        capabilityChecker.ensureReady();
        try {
            vectorStore.delete(new FilterExpressionBuilder().eq(METADATA_MODULE, MODULE_NAME).build());
            log.info("Deleted all vectors for module={}", MODULE_NAME);
            return VectorStoreDeleteResponse.builder()
                    .action("deleteAll")
                    .message("Deleted vectors written by the current module")
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("deleteAll", exception);
        }
    }

    @Override
    public VectorStoreDeleteResponse deleteByFileName(String fileName) {
        capabilityChecker.ensureReady();
        String normalizedFileName = normalizeRequiredText(fileName, "File name must not be blank");
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        try {
            vectorStore.delete(filterExpressionBuilder
                    .and(
                            filterExpressionBuilder.eq(METADATA_MODULE, MODULE_NAME),
                            filterExpressionBuilder.eq(METADATA_FILE_NAME, normalizedFileName))
                    .build());
            log.info("Deleted vectors for fileName={}", normalizedFileName);
            return VectorStoreDeleteResponse.builder()
                    .action("deleteByFileName")
                    .fileName(normalizedFileName)
                    .message("Deleted vectors for the specified file")
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("deleteByFileName", exception);
        }
    }

    /**
     * 分批持久化文档，避免一次写入过多切片导致单次请求过慢或压力过大。
     */
    private void persistDocuments(List<Document> chunkDocuments) {
        List<List<Document>> batches = ParallelExecutionUtils.partition(chunkDocuments, validateWriteBatchSize());
        try {
            if (shouldUseParallelWrite(chunkDocuments.size(), batches.size())) {
                ParallelExecutionUtils.parallelConsumeBatches(batches, commonAsyncExecutor, batch -> persistBatch(batch, 0));
            } else {
                batches.forEach(batch -> persistBatch(batch, 0));
            }
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("upload", exception);
        }
    }

    private void persistBatch(List<Document> batch, int splitDepth) {
        try {
            vectorStore.add(batch);
        } catch (RuntimeException exception) {
            Throwable rootCause = unwrapCause(exception);
            if (isTokenLimitException(rootCause) && splitDepth < MAX_ADAPTIVE_SPLIT_DEPTH) {
                List<Document> smallerChunks = adaptiveSplit(batch, splitDepth);
                if (smallerChunks.size() > batch.size()) {
                    log.warn("Embedding token limit reached, retrying with smaller chunks. splitDepth={}, originalSize={}, newSize={}",
                            splitDepth, batch.size(), smallerChunks.size());
                    List<List<Document>> smallerBatches = ParallelExecutionUtils.partition(smallerChunks, validateWriteBatchSize());
                    smallerBatches.forEach(nextBatch -> persistBatch(nextBatch, splitDepth + 1));
                    return;
                }
            }
            throw exception;
        }
    }

    /**
     * 判断是否应该使用并行写入策略
     *
     * @param chunkCount 数据块的数量
     * @param batchCount 批次数量
     * @return 如果满足以下条件则返回true： 1. 并行写入功能已启用 2. 数据块数量达到并行写入阈值 3. 批次数量大于1 否则返回false
     */
    private boolean shouldUseParallelWrite(int chunkCount, int batchCount) {

        return vectorStoreProperties.isParallelWriteEnabled()
                && chunkCount >= vectorStoreProperties.getParallelWriteThreshold()
                && batchCount > 1;
    }

    private int validateWriteBatchSize() {
        int batchSize = vectorStoreProperties.getWriteBatchSize();
        if (batchSize <= 0) {
            throw VectorStoreException.badRequest("writeBatchSize must be greater than 0");
        }
        return batchSize;
    }

    private List<Document> adaptiveSplit(List<Document> batch, int splitDepth) {
        int adaptiveChunkSize = Math.max(
                MIN_ADAPTIVE_CHUNK_SIZE,
                vectorStoreProperties.getChunkSize() / (int) Math.pow(2, splitDepth + 1));
        TokenTextSplitter adaptiveSplitter = TokenTextSplitter.builder()
                .withChunkSize(adaptiveChunkSize)
                .withMinChunkSizeChars(Math.max(80, vectorStoreProperties.getMinChunkSizeChars() / 2))
                .withMinChunkLengthToEmbed(vectorStoreProperties.getMinChunkLengthToEmbed())
                .withMaxNumChunks(Math.max(vectorStoreProperties.getMaxNumChunks(), batch.size() * 4))
                .withKeepSeparator(vectorStoreProperties.isKeepSeparator())
                .build();
        return adaptiveSplitter.apply(batch);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw VectorStoreException.badRequest("Uploaded file must not be empty");
        }
    }

    private String requireFileName(MultipartFile file) {
        return normalizeRequiredText(file.getOriginalFilename(), "Unable to resolve uploaded file name");
    }

    private String resolveExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw VectorStoreException.badRequest("Unable to resolve file extension");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private Document enrichDocument(
            Document document,
            MultipartFile file,
            String fileName,
            String extension,
            String uploadedAt) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(METADATA_MODULE, MODULE_NAME);
        metadata.put(METADATA_FILE_NAME, fileName);
        metadata.put(METADATA_EXTENSION, extension);
        metadata.put(METADATA_CONTENT_TYPE, resolveContentType(file));
        metadata.put(METADATA_FILE_SIZE, file.getSize());
        metadata.put(METADATA_UPLOADED_AT, uploadedAt);
        if (document.getMetadata() != null) {
            metadata.putAll(document.getMetadata());
        }
        metadata.putIfAbsent(METADATA_SOURCE, fileName);
        metadata.put(METADATA_MODULE, MODULE_NAME);

        return Document.builder()
                .id(document.getId())
                .text(document.getText())
                .metadata(metadata)
                .build();
    }

    private boolean hasTextContent(Document document) {
        return StringUtils.hasText(document.getText());
    }

    private Expression buildSearchFilter(String fileName) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        var moduleFilter = filterExpressionBuilder.eq(METADATA_MODULE, MODULE_NAME);
        if (!StringUtils.hasText(fileName)) {
            return moduleFilter.build();
        }
        return filterExpressionBuilder
                .and(moduleFilter, filterExpressionBuilder.eq(METADATA_FILE_NAME, fileName))
                .build();
    }

    private int validateTopK(Integer topK) {
        int resolvedTopK = topK == null ? vectorStoreProperties.getDefaultTopK() : topK;
        if (resolvedTopK <= 0) {
            throw VectorStoreException.badRequest("topK must be greater than 0");
        }
        return resolvedTopK;
    }

    private Double validateSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null) {
            return null;
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw VectorStoreException.badRequest("similarityThreshold must be between 0 and 1");
        }
        return similarityThreshold;
    }

    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_CONTENT_TYPE;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw VectorStoreException.badRequest(errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private VectorStoreDocumentResponse toDocumentResponse(Document document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (document.getMetadata() != null) {
            metadata.putAll(document.getMetadata());
        }
        return VectorStoreDocumentResponse.builder()
                .id(document.getId())
                .content(document.getText())
                .score(document.getScore())
                .metadata(metadata)
                .build();
    }

    /**
     * 翻译底层 Redis VectorStore 异常，输出更明确的业务提示。
     */
    private VectorStoreException translateVectorStoreException(String action, RuntimeException exception) {
        Throwable rootCause = unwrapCause(exception);
        String message = rootCause.getMessage();
        log.error("Vector store operation failed during {}, rootCause={}", action, message, exception);
        if (rootCause instanceof VectorStoreException vectorStoreException) {
            return vectorStoreException;
        }
        if (isTokenLimitException(rootCause)) {
            return new VectorStoreException(
                    HttpStatus.BAD_REQUEST,
                    "Embedding input is too large during " + action
                            + ". Reduce chunk size or upload a smaller document.",
                    exception);
        }
        if (message != null && (message.contains("HTTP 429")
                || message.contains("AllocationQuota")
                || message.contains("Throttling.AllocationQuota"))) {
            return new VectorStoreException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Embedding model quota exceeded during " + action
                            + ". Reduce upload concurrency or chunk count, or increase DashScope quota.",
                    exception);
        }
        if (message != null && (message.contains("JSON.SET") || message.contains("FT.SEARCH") || message.contains("FT.CREATE"))) {
            return new VectorStoreException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Redis vector store command is unavailable during " + action
                            + ". Please use Redis Stack or install RedisJSON and RediSearch modules.",
                    exception);
        }
        return VectorStoreException.internalError("Vector store operation failed during " + action, exception);
    }

    private boolean isTokenLimitException(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message != null && (
                message.contains("maximum number of allowed input tokens")
                        || message.contains("exceeds the maximum number of allowed input tokens")
                        || message.contains("input tokens"));
    }

    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
