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
import com.spring.ai.vectorstore.search.HybridVectorSearchService;
import com.spring.ai.vectorstore.splitter.VectorDocumentSplitter;
import com.spring.ai.vectorstore.store.VectorStoreGateway;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
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
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
    // 注入向量存储网关
    @Resource
    private VectorStoreGateway vectorStoreGateway;

    // 注入文档切分服务
    @Resource
    private VectorDocumentSplitter vectorDocumentSplitter;

    // 注入向量存储配置属性
    @Resource
    private VectorStoreProperties vectorStoreProperties;

    // 注入多文档阅读器注册表
    @Resource
    private MultipartDocumentReaderRegistry readerRegistry;

    // 注入混合检索服务
    @Resource
    private HybridVectorSearchService hybridVectorSearchService;

    // 注入向量存储文件记录服务
    @Resource
    private VectorStoreFileRecordService vectorStoreFileRecordService;
    // 注入向量存储文件记录服务
    @Resource
    private BigFileService bigFileService;

    // 注入公共异步执行器
    @Resource(name = CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor commonAsyncExecutor;


    /**
     * 上传文件到向量存储
     *
     * @param file 前端上传的 MultipartFile。必须包含原始文件名、文件内容和可选 contentType。
     * @return 上传响应结果，包含文件名、后缀、源文档数量、切片数量、文件大小和上传时间。
     *
     * <p>处理步骤：</p>
     * <p>1. 校验文件不能为空。</p>
     * <p>2. 进入统一存储流程 storeMultipartFile，完成解析、切分、向量化和入库。</p>
     */
    @Override
    public VectorStoreUploadResponse upload(MultipartFile file) {
        validateFile(file);
        return storeMultipartFile(file);
    }

    @Override
    public VectorStoreUploadResponse importBigFile(String fileId) {
        // fileId：大文件模块中已经完成分片合并的文件唯一标识。
        // 先从 bigFileService 读取已完成文件，再包装成 MultipartFile，复用普通上传入库流程。
        BigFileResourceResponse resource = bigFileService.getCompletedFile(normalizeRequiredText(fileId, "文件唯一标识不能为空"));
        MultipartFile file = new PathMultipartFile(resource);
        return storeMultipartFile(file);
    }

    private VectorStoreUploadResponse storeMultipartFile(MultipartFile file) {
        // 第一步：检查当前向量库是否可用。Redis 会检查模块能力，FAISS 会检查 EmbeddingModel。
        vectorStoreGateway.ensureReady();

        // fileName：原始文件名，用于文件台账、metadata 和后续按文件过滤检索。
        String fileName = requireFileName(file);
        // extension：文件后缀，用于选择文档 reader 和切分策略。
        String extension = resolveExtension(fileName);
        // uploadedAt：上传时间，写入 metadata 和文件台账，便于管理页展示。
        String uploadedAt = Instant.now().toString();

        // 第二步：根据文件后缀选择对应 reader，例如 PDF、Word、Excel、文本等。
        MultipartDocumentReader reader = readerRegistry.getReader(extension);
        // sourceDocuments：reader 解析出来的原始文档列表，此时通常还没有标准化 metadata。
        List<Document> sourceDocuments = reader.read(file);
        if (sourceDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档解析后未获取到有效内容");
        }

        // 第三步：标准化文档。给每个 Document 补充模块名、文件名、后缀、文件大小、上传时间等元数据。
        List<Document> normalizedDocuments = sourceDocuments.stream()
                .map(document -> enrichDocument(document, file, fileName, extension, uploadedAt))
                .filter(this::hasTextContent)
                .toList();
        if (normalizedDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档标准化后内容为空");
        }

        // 第四步：分块处理文档。切分服务会根据文件类型和 YAML 配置选择固定、递归、语义或类型感知切分。
        List<Document> chunkDocuments = vectorDocumentSplitter.split(normalizedDocuments, extension);
        if (chunkDocuments.isEmpty()) {
            throw VectorStoreException.badRequest("文档切分后未生成任何分片");
        }

        // 第五步：持久化文档。网关会根据当前 storeType 写入 Redis、Qdrant 或 FAISS。
        persistDocuments(chunkDocuments);
        // 第六步：保存或更新文件台账。台账只保存文件维度统计，具体切片在向量库中。
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
     *
     * @param fileName 文件名过滤条件。不能为空，必须和上传时 metadata 中保存的文件名一致。
     * @return 文档列表响应结果，包含该文件下所有切片内容和元数据。
     *
     * <p>处理含义：</p>
     * <p>1. 该接口主要用于管理页查看某个文件被切成了哪些 chunk。</p>
     * <p>2. Redis 和 FAISS 可以枚举已有切片；Qdrant 默认只能枚举运行期镜像。</p>
     * <p>3. 返回前按切片 id 排序，便于前端稳定展示。</p>
     */
    @Override
    public VectorStoreDocumentListResponse listDocuments(String fileName) {
        vectorStoreGateway.ensureReady();
        String normalizedFileName = normalizeRequiredText(fileName, "文件名不能为空");

        try {
            List<VectorStoreDocumentResponse> items = vectorStoreGateway.listDocuments(normalizedFileName).stream()
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
     *
     * @param query 用户检索内容，不能为空。该文本会同时作为向量检索 query 和关键词检索 query。
     * @param fileName 文件名过滤条件，可为空。为空表示在当前模块全部文件中检索。
     * @param topK 返回结果数量。为空时使用 app.vector-store.default-top-k。
     * @param similarityThreshold 向量相似度阈值，可为空。非空时必须在 0 到 1 之间。
     * @return 搜索响应结果，包含最终命中文档列表、topK、阈值和文件过滤条件。
     *
     * <p>处理步骤：</p>
     * <p>1. 校验向量库可用，并规范化 query、fileName、topK、similarityThreshold。</p>
     * <p>2. 构造 SearchRequest，写入 query、topK、相似度阈值和模块/文件过滤表达式。</p>
     * <p>3. 调用 HybridVectorSearchService，内部执行向量检索、关键词检索、RRF 融合和可选 Rerank。</p>
     * <p>4. 将 Spring AI Document 转成接口响应对象。</p>
     */
    @Override
    public VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold) {
        vectorStoreGateway.ensureReady();

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
            List<VectorStoreDocumentResponse> items = hybridVectorSearchService.search(builder.build(), normalizedFileName).stream()
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
        vectorStoreGateway.ensureReady();
        try {
            vectorStoreGateway.delete(new FilterExpressionBuilder().eq(METADATA_MODULE, MODULE_NAME).build());
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
        vectorStoreGateway.ensureReady();
        String normalizedFileName = normalizeRequiredText(fileName, "File name must not be blank");
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        try {
            vectorStoreGateway.delete(filterExpressionBuilder
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
     *
     * @param chunkDocuments 分块后的文档列表。每个文档都是最终需要进入向量库的 chunk。
     *
     * <p>处理含义：</p>
     * <p>1. 根据 writeBatchSize 将 chunk 列表拆成多个批次，避免单次写入过大。</p>
     * <p>2. 如果 parallelWriteEnabled=true 且切片数量达到 parallelWriteThreshold，则使用公共线程池并行写入。</p>
     * <p>3. 如果未达到并行条件，则按批次串行写入，降低小文件上传时的线程调度成本。</p>
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
     *
     * @param batch 当前批次文档。该批次会一次性传给 VectorStoreGateway 写入。
     * @param splitDepth 自适应切分深度。首次写入为 0，遇到嵌入模型 token 超限后递增。
     *
     * <p>处理含义：</p>
     * <p>1. 正常情况下直接调用 vectorStoreGateway.add 写入向量库。</p>
     * <p>2. 如果模型报 token 超限，并且还没有超过最大自适应深度，则把当前批次切得更小后重试。</p>
     * <p>3. 如果重试后仍然无法变小，或者不是 token 超限错误，则继续抛出异常。</p>
     */
    private void persistBatch(List<Document> batch, int splitDepth) {
        try {
            vectorStoreGateway.add(batch);
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
     *
     * @param batch 写入失败的原始批次。通常是因为某些 chunk 对嵌入模型来说仍然过长。
     * @param splitDepth 当前自适应切分深度，用于计算下一轮更小的 chunkSize。
     * @return 更细粒度的文档切片列表。
     *
     * <p>处理含义：</p>
     * <p>1. 每深入一层，chunkSize 约减半，但不会低于 MIN_ADAPTIVE_CHUNK_SIZE。</p>
     * <p>2. 使用 TokenTextSplitter 重新切分当前批次。</p>
     * <p>3. maxNumChunks 会结合原配置和批次大小放大，避免自适应切分被数量上限过早截断。</p>
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
        Map<String, Object> metadata = document.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(document.getMetadata());
        return VectorStoreDocumentResponse.builder()
                .id(document.getId())
                .content(document.getText())
                .score(document.getScore())
                .metadata(metadata)
                .build();
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






