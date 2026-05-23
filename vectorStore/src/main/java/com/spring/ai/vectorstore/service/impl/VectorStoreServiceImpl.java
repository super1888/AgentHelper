package com.spring.ai.vectorstore.service.impl;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_CONTENT_TYPE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_EXTENSION;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_SIZE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_SOURCE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_UPLOADED_AT;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.bigfile.domain.response.BigFileResourceResponse;
import com.spring.ai.bigfile.service.BigFileService;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.repository.enitiy.VectorStoreFileRecord;
import com.spring.ai.common.repository.service.VectorStoreFileRecordService;
import com.spring.ai.common.utils.ParallelExecutionUtils;
import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreStatisticsResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import com.spring.ai.vectorstore.reader.MultipartDocumentReader;
import com.spring.ai.vectorstore.reader.MultipartDocumentReaderRegistry;
import com.spring.ai.vectorstore.service.VectorStoreService;
import com.spring.ai.vectorstore.store.RedisVectorStoreCapabilityChecker;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
 * 向量存储服务实现类，提供文档上传、检索、删除等功能
 */
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    // 默认内容类型
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    // 存储状态：活跃
    private static final String STORE_STATUS_ACTIVE = "ACTIVE";
    // 存储状态：已删除
    private static final String STORE_STATUS_DELETED = "DELETED";
    // 最小自适应分块大小
    private static final int MIN_ADAPTIVE_CHUNK_SIZE = 100;
    // 最大自适应分割深度
    private static final int MAX_ADAPTIVE_SPLIT_DEPTH = 3;
    // 文件列表扫描数量
    private static final int FILE_LIST_SCAN_COUNT = 200;

    private static class PathMultipartFile implements MultipartFile {

        private final BigFileResourceResponse resource;

        private PathMultipartFile(BigFileResourceResponse resource) {
            this.resource = resource;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return resource.getFileName();
        }

        @Override
        public String getContentType() {
            return StringUtils.hasText(resource.getContentType()) ? resource.getContentType() : DEFAULT_CONTENT_TYPE;
        }

        @Override
        public boolean isEmpty() {
            return getSize() <= 0;
        }

        @Override
        public long getSize() {
            return resource.getFileSize() == null ? 0L : resource.getFileSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(resource.getStoragePath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(resource.getStoragePath());
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.copy(resource.getStoragePath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    // 注入向量存储组件
    @Resource
    private VectorStore vectorStore;

    // 注入文本分割器
    @Resource
    private TokenTextSplitter tokenTextSplitter;

    // 注入向量存储配置属性
    @Resource
    private VectorStoreProperties vectorStoreProperties;

    // 注入多文档阅读器注册表
    @Resource
    private MultipartDocumentReaderRegistry readerRegistry;

    // 注入Redis向量存储能力检查器
    @Resource
    private RedisVectorStoreCapabilityChecker capabilityChecker;

    // 注入向量存储文件记录服务
    @Resource
    private VectorStoreFileRecordService vectorStoreFileRecordService;
    // 注入向量存储文件记录服务
    @Resource
    private BigFileService bigFileService;

    // 注入公共异步执行器
    @Resource(name = CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;

    // Redis向量存储前缀
    @Value("${spring.ai.vectorstore.redis.prefix:vector:}")
    private String redisVectorPrefix;

    /**
     * 上传文件到向量存储
     * @param file 上传的文件
     * @return 上传响应结果
     */
    @Override
    public VectorStoreUploadResponse upload(MultipartFile file) {
        validateFile(file);
        return storeMultipartFile(file);
    }

    @Override
    public VectorStoreUploadResponse importBigFile(String fileId) {
        BigFileResourceResponse resource = bigFileService.getCompletedFile(normalizeRequiredText(fileId, "文件唯一标识不能为空"));
        MultipartFile file = new PathMultipartFile(resource);
        return storeMultipartFile(file);
    }

    private VectorStoreUploadResponse storeMultipartFile(MultipartFile file) {
        capabilityChecker.ensureReady();

        String fileName = requireFileName(file);
        String extension = resolveExtension(fileName);
        String uploadedAt = Instant.now().toString();

        MultipartDocumentReader reader = readerRegistry.getReader(extension);
        List<Document> sourceDocuments = reader.read(file);
        if (sourceDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档解析后未获取到有效内容");
        }

        // 标准化文档
        List<Document> normalizedDocuments = sourceDocuments.stream()
                .map(document -> enrichDocument(document, file, fileName, extension, uploadedAt))
                .filter(this::hasTextContent)
                .toList();
        if (normalizedDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档标准化后内容为空");
        }

        // 分块处理文档
        List<Document> chunkDocuments = tokenTextSplitter.apply(normalizedDocuments);
        if (chunkDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档切分后未生成任何分片");
        }

        // 持久化文档
        persistDocuments(chunkDocuments);
        // 保存或更新文件记录
        saveOrUpdateFileRecord(file, fileName, extension, uploadedAt, sourceDocuments.size(), chunkDocuments.size());
        log.info("Stored vector document, fileName={}, sourceDocuments={}, chunks={}",
                fileName, sourceDocuments.size(), chunkDocuments.size());

        return VectorStoreUploadResponse.builder()
                .fileName(fileName)
                .fileExtension(extension)
                .sourceDocumentCount(sourceDocuments.size())
                .chunkCount(chunkDocuments.size())
                .fileSize(file.getSize())
                .uploadedAt(uploadedAt)
                .message("文档解析、切分并入库成功")
                .build();
    }

    /**
     * 列出所有文件
     * @return 文件列表响应结果
     */
    @Override
    public VectorStoreFileListResponse listFiles() {
        List<VectorStoreFileResponse> items = vectorStoreFileRecordService.listByModule(MODULE_NAME).stream()
                .map(this::toFileResponse)
                .toList();

        return VectorStoreFileListResponse.builder()
                .total(items.size())
                .items(items)
                .build();
    }

    /**
     * 列出指定文件的所有文档
     * @param fileName 文件名
     * @return 文档列表响应结果
     */
    @Override
    public VectorStoreDocumentListResponse listDocuments(String fileName) {
        capabilityChecker.ensureReady();
        String normalizedFileName = normalizeRequiredText(fileName, "文件名不能为空");

        try {
            JedisPooled jedis = resolveJedisClient();
            List<VectorStoreDocumentResponse> items = scanVectorKeys(jedis).stream()
                    .map(key -> readStoredDocument(jedis, key))
                    .filter(documentMap -> MODULE_NAME.equals(asString(documentMap.get(METADATA_MODULE))))
                    .filter(documentMap -> normalizedFileName.equals(asString(documentMap.get(METADATA_FILE_NAME))))
                    .map(this::toDocumentResponse)
                    .sorted(Comparator.comparing(VectorStoreDocumentResponse::getId, Comparator.nullsLast(String::compareTo)))
                    .toList();

            return VectorStoreDocumentListResponse.builder()
                    .fileName(normalizedFileName)
                    .total(items.size())
                    .items(items)
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("listDocuments", exception);
        }
    }

    /**
     * 搜索文档
     * @param query 查询内容
     * @param fileName 文件名（可选）
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值
     * @return 搜索响应结果
     */
    @Override
    public VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold) {
        capabilityChecker.ensureReady();

        String normalizedQuery = normalizeRequiredText(query, "检索内容不能为空");
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

    /**
     * 获取统计信息
     * @return 统计响应结果
     */
    @Override
    public VectorStoreStatisticsResponse statistics() {
        List<VectorStoreFileRecord> records = vectorStoreFileRecordService.listByModule(MODULE_NAME);
        int totalChunks = records.stream()
                .map(VectorStoreFileRecord::getChunkCount)
                .filter(value -> value != null && value > 0)
                .mapToInt(Integer::intValue)
                .sum();
        long totalFileSize = records.stream()
                .map(VectorStoreFileRecord::getFileSize)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        return VectorStoreStatisticsResponse.builder()
                .totalFiles(records.size())
                .activeFiles((int) vectorStoreFileRecordService.countByModuleAndStatus(MODULE_NAME, STORE_STATUS_ACTIVE))
                .deletedFiles((int) vectorStoreFileRecordService.countByModuleAndStatus(MODULE_NAME, STORE_STATUS_DELETED))
                .totalChunks(totalChunks)
                .totalFileSize(totalFileSize)
                .build();
    }

    /**
     * 删除所有向量数据
     * @return 删除响应结果
     */
    @Override
    public VectorStoreDeleteResponse deleteAll() {
        capabilityChecker.ensureReady();
        try {
            vectorStore.delete(new FilterExpressionBuilder().eq(METADATA_MODULE, MODULE_NAME).build());
            markAllRecordsDeleted();
            log.info("Deleted all vectors for module={}", MODULE_NAME);
            return VectorStoreDeleteResponse.builder()
                    .action("deleteAll")
                    .message("清空当前模块向量数据成功！")
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("deleteAll", exception);
        }
    }

    /**
     * 根据文件名删除向量数据
     * @param fileName 文件名
     * @return 删除响应结果
     */
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
            markRecordDeleted(normalizedFileName);
            log.info("Deleted vectors for fileName={}", normalizedFileName);
            return VectorStoreDeleteResponse.builder()
                    .action("deleteByFileName")
                    .fileName(normalizedFileName)
                    .message("已删除指定文件的向量数据")
                    .build();
        } catch (RuntimeException exception) {
            throw translateVectorStoreException("deleteByFileName", exception);
        }
    }

    /**
     * 持久化文档列表
     * @param chunkDocuments 分块后的文档列表
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
     * 持久化文档批次
     * @param batch 文档批次
     * @param splitDepth 分割深度
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
     * 判断是否使用并行写入
     * @param chunkCount 分块数量
     * @param batchCount 批次数量
     * @return 是否使用并行写入
     */
    private boolean shouldUseParallelWrite(int chunkCount, int batchCount) {
        return vectorStoreProperties.isParallelWriteEnabled()
                && chunkCount >= vectorStoreProperties.getParallelWriteThreshold()
                && batchCount > 1;
    }

    /**
     * 验证写入批次大小
     * @return 批次大小
     */
    private int validateWriteBatchSize() {
        int batchSize = vectorStoreProperties.getWriteBatchSize();
        if (batchSize <= 0) {
            throw VectorStoreException.badRequest("writeBatchSize 必须大于 0");
        }
        return batchSize;
    }

    /**
     * 自适应分割文档
     * @param batch 文档批次
     * @param splitDepth 分割深度
     * @return 分割后的文档列表
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
     * 验证上传文件
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw VectorStoreException.badRequest("上传文件不能为空");
        }
    }

    /**
     * 获取文件名
     * @param file 上传的文件
     * @return 文件名
     */
    private String requireFileName(MultipartFile file) {
        return normalizeRequiredText(file.getOriginalFilename(), "无法获取上传文件名");
    }

    /**
     * 解析文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String resolveExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw VectorStoreException.badRequest("无法解析文件扩展名");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /**
     * 丰富文档元数据
     * @param document 原始文档
     * @param file 上传的文件
     * @param fileName 文件名
     * @param extension 文件扩展名
     * @param uploadedAt 上传时间
     * @return 丰富后的文档
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
     * 检查文档是否包含文本内容
     * @param document 文档
     * @return 是否包含文本内容
     */
    private boolean hasTextContent(Document document) {
        return StringUtils.hasText(document.getText());
    }

    /**
     * 构建搜索过滤器
     * @param fileName 文件名
     * @return 过滤表达式
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
     * 验证topK参数
     * @param topK topK值
     * @return 验证后的topK值
     */
    private int validateTopK(Integer topK) {
        int resolvedTopK = topK == null ? vectorStoreProperties.getDefaultTopK() : topK;
        if (resolvedTopK <= 0) {
            throw VectorStoreException.badRequest("TopK 必须大于 0");
        }
        return resolvedTopK;
    }

    /**
     * 验证相似度阈值
     * @param similarityThreshold 相似度阈值
     * @return 验证后的相似度阈值
     */
    private Double validateSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null) {
            return null;
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw VectorStoreException.badRequest("相似度阈值必须在 0 到 1 之间");
        }
        return similarityThreshold;
    }

    /**
     * 解析内容类型
     * @param file 上传的文件
     * @return 内容类型
     */
    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_CONTENT_TYPE;
    }

    /**
     * 规范化必需的文本
     * @param value 文本值
     * @param errorMessage 错误消息
     * @return 规范化后的文本
     */
    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw VectorStoreException.badRequest(errorMessage);
        }
        return value.trim();
    }

    /**
     * 规范化可选的文本
     * @param value 文本值
     * @return 规范化后的文本
     */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 解析Jedis客户端
     * @return Jedis客户端
     */
    private JedisPooled resolveJedisClient() {
        return vectorStore.getNativeClient()
                .filter(JedisPooled.class::isInstance)
                .map(JedisPooled.class::cast)
                .orElseThrow(() -> new VectorStoreException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "当前向量存储未提供 Jedis 客户端"));
    }

    /**
     * 扫描向量键
     * @param jedis Jedis客户端
     * @return 向量键列表
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
     * 读取存储的文档
     * @param jedis Jedis客户端
     * @param key 文档键
     * @return 文档映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readStoredDocument(JedisPooled jedis, String key) {
        Object jsonObject = jedis.jsonGet(key);
        if (jsonObject instanceof Map<?, ?> jsonMap) {
            return (Map<String, Object>) jsonMap;
        }
        return Map.of();
    }

    /**
     * 保存或更新文件记录
     * @param file 上传的文件
     * @param fileName 文件名
     * @param extension 文件扩展名
     * @param uploadedAt 上传时间
     * @param sourceDocumentCount 源文档数量
     * @param chunkCount 分块数量
     */
    private void saveOrUpdateFileRecord(
            MultipartFile file,
            String fileName,
            String extension,
            String uploadedAt,
            int sourceDocumentCount,
            int chunkCount
    ) {
        VectorStoreFileRecord record = vectorStoreFileRecordService.getByModuleAndFileName(MODULE_NAME, fileName);
        if (record == null) {
            record = new VectorStoreFileRecord();
            record.setModuleName(MODULE_NAME);
            record.setFileName(fileName);
        }
        record.setFileExtension(extension);
        record.setContentType(resolveContentType(file));
        record.setFileSize(file.getSize());
        record.setSourceDocumentCount(sourceDocumentCount);
        record.setChunkCount(chunkCount);
        record.setUploadedAt(uploadedAt);
        record.setStoreStatus(STORE_STATUS_ACTIVE);
        record.setLastOperationMessage("文件上传并建立索引成功");
        vectorStoreFileRecordService.saveOrUpdate(record);
    }

    /**
     * 标记记录为已删除
     * @param fileName 文件名
     */
    private void markRecordDeleted(String fileName) {
        VectorStoreFileRecord record = vectorStoreFileRecordService.getByModuleAndFileName(MODULE_NAME, fileName);
        if (record == null) {
            return;
        }
        record.setStoreStatus(STORE_STATUS_DELETED);
        record.setLastOperationMessage("已按文件名删除向量数据");
        vectorStoreFileRecordService.updateById(record);
    }

    /**
     * 标记所有记录为已删除
     */
    private void markAllRecordsDeleted() {
        List<VectorStoreFileRecord> records = vectorStoreFileRecordService.listByModule(MODULE_NAME);
        if (records.isEmpty()) {
            return;
        }
        records.forEach(record -> {
            record.setStoreStatus(STORE_STATUS_DELETED);
            record.setLastOperationMessage("已批量删除向量数据");
        });
        vectorStoreFileRecordService.updateBatchById(records);
    }

    /**
     * 转换文件记录为响应对象
     * @param record 文件记录
     * @return 文件响应对象
     */
    private VectorStoreFileResponse toFileResponse(VectorStoreFileRecord record) {
        return VectorStoreFileResponse.builder()
                .id(record.getId())
                .fileName(record.getFileName())
                .fileExtension(record.getFileExtension())
                .contentType(record.getContentType())
                .fileSize(record.getFileSize())
                .sourceDocumentCount(record.getSourceDocumentCount())
                .chunkCount(record.getChunkCount())
                .uploadedAt(record.getUploadedAt())
                .storeStatus(record.getStoreStatus())
                .lastOperationMessage(record.getLastOperationMessage())
                .build();
    }

    /**
     * 转换文档为响应对象
     * @param document 文档
     * @return 文档响应对象
     */
    private VectorStoreDocumentResponse toDocumentResponse(Document document) {
        document.getMetadata();
        Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
        return VectorStoreDocumentResponse.builder()
                .id(document.getId())
                .content(document.getText())
                .score(document.getScore())
                .metadata(metadata)
                .build();
    }

    /**
     * 转换文档映射为响应对象
     * @param documentMap 文档映射
     * @return 文档响应对象
     */
    private VectorStoreDocumentResponse toDocumentResponse(Map<String, Object> documentMap) {
        Object content = documentMap.getOrDefault("content", documentMap.get("text"));
        Map<String, Object> metadata = extractMetadata(documentMap);
        return VectorStoreDocumentResponse.builder()
                .id(asString(documentMap.get("id")))
                .content(asString(content))
                .metadata(metadata)
                .build();
    }

    /**
     * 提取文档元数据
     * @param documentMap 文档映射
     * @return 元数据映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Map<String, Object> documentMap) {
        Object metadata = documentMap.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap) {
            return (Map<String, Object>) metadataMap;
        }
        Map<String, Object> fallbackMetadata = new LinkedHashMap<>();
        documentMap.forEach((key, value) -> {
            if (key.startsWith("metadata.") || METADATA_FILE_NAME.equals(key) || METADATA_MODULE.equals(key)) {
                fallbackMetadata.put(key, value);
            }
        });
        return fallbackMetadata;
    }

    /**
     * 将对象转换为字符串
     * @param value 对象值
     * @return 字符串值
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 转换向量存储异常
     * @return 向量存储异常
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
                    "执行" + action + "时嵌入输入内容过大，请减小分片大小或上传更小的文档。",
                    exception);
        }
        if (message != null && (message.contains("HTTP 429")
                || message.contains("AllocationQuota")
                || message.contains("Throttling.AllocationQuota"))) {
            return new VectorStoreException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "执行" + action + "时嵌入模型额度已超限，请降低上传并发或分片数量，或提升 DashScope 配额。",
                    exception);
        }
        if (message != null && (message.contains("JSON.SET") || message.contains("FT.SEARCH") || message.contains("FT.CREATE"))) {
            return new VectorStoreException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "执行" + action + "时 Redis 向量库命令不可用，请使用 Redis Stack 或安装 RedisJSON 与 RediSearch 模块。",
                    exception);
        }
        if (message != null && (message.contains("no such index") || message.contains("Unknown Index name"))) {
            return new VectorStoreException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "执行" + action + "时未找到 Redis 向量索引，请开启 spring.ai.vectorstore.redis.initialize-schema=true 后重启应用。",
                    exception);
        }
        return VectorStoreException.internalError("执行" + action + "时向量库操作失败", exception);
    }

    /**
     * 检查是否为令牌限制异常
     * @param throwable 异常对象
     * @return 是否为令牌限制异常
     */
    private boolean isTokenLimitException(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message != null && (
                message.contains("maximum number of allowed input tokens")
                        || message.contains("exceeds the maximum number of allowed input tokens")
                        || message.contains("input tokens"));
    }

    /**
     * 解包异常原因
     * @param throwable 异常对象
     * @return 根本原因
     */
    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
