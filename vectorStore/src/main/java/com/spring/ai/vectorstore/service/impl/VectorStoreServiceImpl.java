package com.spring.ai.vectorstore.service.impl;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_CONTENT_TYPE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_EXTENSION;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_SIZE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_SOURCE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_UPLOADED_AT;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.utils.ParallelExecutionUtils;
import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import com.spring.ai.vectorstore.reader.MultipartDocumentReader;
import com.spring.ai.vectorstore.reader.MultipartDocumentReaderRegistry;
import com.spring.ai.vectorstore.service.VectorStoreService;
import com.spring.ai.vectorstore.store.RedisVectorStoreCapabilityChecker;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * 文件用途：向量存储服务实现类
 * 作者：Codex
 * 创建时间：2026-04-16
 * 核心功能：负责文件解析、切片入库、文件列表聚合、语义检索与删除操作。
 */
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final int MIN_ADAPTIVE_CHUNK_SIZE = 100;
    private static final int MAX_ADAPTIVE_SPLIT_DEPTH = 3;
    private static final int FILE_LIST_SCAN_COUNT = 200;
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

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

    @Resource
    private ObjectMapper objectMapper;

    @Resource(name = CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;

    @Value("${spring.ai.vectorstore.redis.prefix:vector:}")
    private String redisVectorPrefix;

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
    public VectorStoreFileListResponse listFiles() {
        capabilityChecker.ensureReady();

        try {
            JedisPooled jedis = resolveJedisClient();
            Map<String, VectorStoreFileResponse> fileMap = new LinkedHashMap<>();

            for (String key : scanVectorKeys(jedis)) {
                Map<String, Object> documentMap = readStoredDocument(jedis, key);
                if (!MODULE_NAME.equals(asString(documentMap.get(METADATA_MODULE)))) {
                    continue;
                }

                String fileName = normalizeOptionalText(asString(documentMap.get(METADATA_FILE_NAME)));
                if (!StringUtils.hasText(fileName)) {
                    continue;
                }

                VectorStoreFileResponse fileResponse = fileMap.computeIfAbsent(fileName, value ->
                        VectorStoreFileResponse.builder()
                                .fileName(value)
                                .chunkCount(0)
                                .build());
                mergeFileSummary(fileResponse, documentMap);
            }

            List<VectorStoreFileResponse> items = new ArrayList<>(fileMap.values());
            items.sort(Comparator
                    .comparing(VectorStoreFileResponse::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(VectorStoreFileResponse::getFileName, Comparator.nullsLast(String::compareToIgnoreCase)));

            return VectorStoreFileListResponse.builder()
                    .total(items.size())
                    .items(items)
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("listFiles", exception);
        }
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
     * 分批持久化文档，避免单次写入过多切片导致请求过慢。
     *
     * @param chunkDocuments 切片列表
     * @returns void
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

    /**
     * 按批次写入向量库，并在遇到 token 过大时自适应拆分重试。
     *
     * @param batch 当前批次
     * @param splitDepth 当前递归深度
     * @returns void
     */
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
     * 判断是否启用并行批量写入。
     *
     * @param chunkCount 切片数量
     * @param batchCount 批次数量
     * @return 是否启用并行写入
     */
    private boolean shouldUseParallelWrite(int chunkCount, int batchCount) {
        return vectorStoreProperties.isParallelWriteEnabled()
                && chunkCount >= vectorStoreProperties.getParallelWriteThreshold()
                && batchCount > 1;
    }

    /**
     * 校验批量写入大小配置。
     *
     * @return 批量大小
     */
    private int validateWriteBatchSize() {
        int batchSize = vectorStoreProperties.getWriteBatchSize();
        if (batchSize <= 0) {
            throw VectorStoreException.badRequest("writeBatchSize must be greater than 0");
        }
        return batchSize;
    }

    /**
     * 自适应切分文档，缓解 embedding token 过大问题。
     *
     * @param batch 原始批次
     * @param splitDepth 切分深度
     * @return 更细粒度的切片列表
     */
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

    /**
     * 校验上传文件是否为空。
     *
     * @param file 上传文件
     * @returns void
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw VectorStoreException.badRequest("Uploaded file must not be empty");
        }
    }

    /**
     * 获取并校验上传文件名。
     *
     * @param file 上传文件
     * @return 文件名
     */
    private String requireFileName(MultipartFile file) {
        return normalizeRequiredText(file.getOriginalFilename(), "Unable to resolve uploaded file name");
    }

    /**
     * 解析文件后缀。
     *
     * @param fileName 文件名
     * @return 小写后缀名
     */
    private String resolveExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw VectorStoreException.badRequest("Unable to resolve file extension");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /**
     * 补齐文档元数据，确保后续可检索、可管理。
     *
     * @param document 原始文档
     * @param file 上传文件
     * @param fileName 文件名
     * @param extension 文件后缀
     * @param uploadedAt 上传时间
     * @return 补齐后的文档
     */
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

    /**
     * 判断文档是否含有效文本。
     *
     * @param document 文档对象
     * @return 是否存在文本内容
     */
    private boolean hasTextContent(Document document) {
        return StringUtils.hasText(document.getText());
    }

    /**
     * 构建检索过滤条件。
     *
     * @param fileName 文件名过滤条件
     * @return 向量过滤表达式
     */
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

    /**
     * 校验 topK 参数。
     *
     * @param topK 入参 topK
     * @return 实际使用的 topK
     */
    private int validateTopK(Integer topK) {
        int resolvedTopK = topK == null ? vectorStoreProperties.getDefaultTopK() : topK;
        if (resolvedTopK <= 0) {
            throw VectorStoreException.badRequest("topK must be greater than 0");
        }
        return resolvedTopK;
    }

    /**
     * 校验相似度阈值范围。
     *
     * @param similarityThreshold 相似度阈值
     * @return 校验后的阈值
     */
    private Double validateSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null) {
            return null;
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw VectorStoreException.badRequest("similarityThreshold must be between 0 and 1");
        }
        return similarityThreshold;
    }

    /**
     * 解析内容类型。
     *
     * @param file 上传文件
     * @return 内容类型
     */
    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_CONTENT_TYPE;
    }

    /**
     * 规范化必填文本。
     *
     * @param value 原始值
     * @param errorMessage 为空时的错误信息
     * @return 去空格后的文本
     */
    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw VectorStoreException.badRequest(errorMessage);
        }
        return value.trim();
    }

    /**
     * 规范化可选文本。
     *
     * @param value 原始值
     * @return 去空格后的文本，空值返回 null
     */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 解析底层 Jedis 客户端，用于补充 Redis JSON 扫描能力。
     *
     * @return Jedis 客户端
     */
    private JedisPooled resolveJedisClient() {
        return vectorStore.getNativeClient()
                .filter(JedisPooled.class::isInstance)
                .map(JedisPooled.class::cast)
                .orElseThrow(() -> new VectorStoreException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Current vector store does not expose a Jedis client"));
    }

    /**
     * 扫描当前向量前缀下的全部 Redis 键。
     *
     * @param jedis Redis 客户端
     * @return Redis 键列表
     */
    private List<String> scanVectorKeys(JedisPooled jedis) {
        List<String> keys = new ArrayList<>();
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams scanParams = new ScanParams()
                .match(redisVectorPrefix + "*")
                .count(FILE_LIST_SCAN_COUNT);

        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            keys.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        return keys;
    }

    /**
     * 读取 Redis JSON 文档。
     *
     * @param jedis Redis 客户端
     * @param key Redis 键
     * @return 文档 Map
     */
    private Map<String, Object> readStoredDocument(JedisPooled jedis, String key) {
        Object jsonObject = jedis.jsonGet(key);
        if (jsonObject == null) {
            return Map.of();
        }
        return objectMapper.convertValue(jsonObject, MAP_TYPE_REFERENCE);
    }

    /**
     * 合并同一文件的切片元数据。
     *
     * @param target 聚合结果
     * @param documentMap Redis 文档数据
     * @returns void
     */
    private void mergeFileSummary(VectorStoreFileResponse target, Map<String, Object> documentMap) {
        target.setChunkCount((target.getChunkCount() == null ? 0 : target.getChunkCount()) + 1);
        target.setFileExtension(preferNonBlank(target.getFileExtension(), asString(documentMap.get(METADATA_EXTENSION))));
        target.setContentType(preferNonBlank(target.getContentType(), asString(documentMap.get(METADATA_CONTENT_TYPE))));
        target.setFileSize(preferNonNull(target.getFileSize(), asLong(documentMap.get(METADATA_FILE_SIZE))));

        String uploadedAt = asString(documentMap.get(METADATA_UPLOADED_AT));
        if (isLaterTime(uploadedAt, target.getUploadedAt())) {
            target.setUploadedAt(uploadedAt);
        } else if (!StringUtils.hasText(target.getUploadedAt())) {
            target.setUploadedAt(uploadedAt);
        }
    }

    /**
     * 优先保留已有非空文本。
     *
     * @param currentValue 当前值
     * @param candidateValue 候选值
     * @return 选中的值
     */
    private String preferNonBlank(String currentValue, String candidateValue) {
        return StringUtils.hasText(currentValue) ? currentValue : normalizeOptionalText(candidateValue);
    }

    /**
     * 优先保留已有非空对象。
     *
     * @param currentValue 当前值
     * @param candidateValue 候选值
     * @return 选中的值
     */
    private Long preferNonNull(Long currentValue, Long candidateValue) {
        return currentValue != null ? currentValue : candidateValue;
    }

    /**
     * 判断候选时间是否更新。
     *
     * @param candidateValue 候选时间
     * @param currentValue 当前时间
     * @return 候选时间是否更晚
     */
    private boolean isLaterTime(String candidateValue, String currentValue) {
        if (!StringUtils.hasText(candidateValue)) {
            return false;
        }
        if (!StringUtils.hasText(currentValue)) {
            return true;
        }
        return candidateValue.compareTo(currentValue) > 0;
    }

    /**
     * 转换为字符串。
     *
     * @param value 原始值
     * @return 字符串结果
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 转换为 Long，异常时返回 null。
     *
     * @param value 原始值
     * @return Long 结果
     */
    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            log.warn("Unable to parse file size from vector document metadata, value={}", value);
            return null;
        }
    }

    /**
     * 转换搜索命中文档为响应对象。
     *
     * @param document 命中文档
     * @return 响应对象
     */
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
     *
     * @param action 当前动作
     * @param exception 原始异常
     * @return 业务异常
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
        if (message != null && (message.contains("no such index") || message.contains("Unknown Index name"))) {
            return new VectorStoreException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Redis vector index is missing during " + action
                            + ". Enable spring.ai.vectorstore.redis.initialize-schema=true and restart the application.",
                    exception);
        }
        return VectorStoreException.internalError("Vector store operation failed during " + action, exception);
    }

    /**
     * 判断异常是否为 token 超限。
     *
     * @param throwable 异常对象
     * @return 是否 token 超限
     */
    private boolean isTokenLimitException(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message != null && (
                message.contains("maximum number of allowed input tokens")
                        || message.contains("exceeds the maximum number of allowed input tokens")
                        || message.contains("input tokens"));
    }

    /**
     * 展开异常根因。
     *
     * @param throwable 异常对象
     * @return 根因异常
     */
    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
