package com.spring.ai.vectorstore.service.impl;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_CONTENT_TYPE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_EXTENSION;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_SIZE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_SOURCE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_UPLOADED_AT;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import com.spring.ai.vectorstore.reader.MultipartDocumentReader;
import com.spring.ai.vectorstore.reader.MultipartDocumentReaderRegistry;
import com.spring.ai.vectorstore.service.VectorStoreService;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
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

    @Resource
    VectorStore vectorStore;

    @Resource
    TokenTextSplitter tokenTextSplitter;

    @Resource
    VectorStoreProperties vectorStoreProperties;

    @Resource
    MultipartDocumentReaderRegistry readerRegistry;

    /**
     * 上传文件并写入向量库。 整体流程包括：参数校验、按扩展名匹配读取器、提取文本、补齐元数据、文本切片、写入向量库。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @Override
    public VectorStoreUploadResponse upload(MultipartFile file) {
        validateFile(file);
        String fileName = requireFileName(file);
        String extension = resolveExtension(fileName);
        String uploadedAt = Instant.now().toString();

        // 先根据文件扩展名选择具体读取策略，避免业务层感知底层解析实现。
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

        vectorStore.add(chunkDocuments);
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

    /**
     * 检索向量库。 检索时默认只查询当前模块写入的数据，避免误检索到共享向量库中的其他业务数据。
     *
     * @param query               查询词
     * @param fileName            文件名过滤条件
     * @param topK                返回结果数
     * @param similarityThreshold 相似度阈值
     * @return 检索结果
     */
    @Override
    public VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold) {
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
    }

    /**
     * 删除当前模块写入的全部向量数据。
     *
     * @return 删除结果
     */
    @Override
    public VectorStoreDeleteResponse deleteAll() {
        vectorStore.delete(new FilterExpressionBuilder().eq(METADATA_MODULE, MODULE_NAME).build());
        log.info("Deleted all vectors for module={}", MODULE_NAME);
        return VectorStoreDeleteResponse.builder()
                .action("deleteAll")
                .fileName(null)
                .message("Deleted vectors written by the current module")
                .build();
    }

    /**
     * 根据文件名删除向量数据。
     *
     * @param fileName 文件名
     * @return 删除结果
     */
    @Override
    public VectorStoreDeleteResponse deleteByFileName(String fileName) {
        String normalizedFileName = normalizeRequiredText(fileName, "File name must not be blank");
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
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
    }

    /**
     * 校验上传文件不能为空。
     *
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw VectorStoreException.badRequest("Uploaded file must not be empty");
        }
    }

    /**
     * 获取并校验原始文件名。
     *
     * @param file 上传文件
     * @return 原始文件名
     */
    private String requireFileName(MultipartFile file) {
        return normalizeRequiredText(file.getOriginalFilename(), "Unable to resolve uploaded file name");
    }

    /**
     * 解析文件扩展名，并统一转换为小写。
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String resolveExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw VectorStoreException.badRequest("Unable to resolve file extension");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /**
     * 为文档补充统一元数据。 元数据保持扁平结构，便于向量库过滤条件直接使用。
     *
     * @param document   原始文档
     * @param file       上传文件
     * @param fileName   文件名
     * @param extension  扩展名
     * @param uploadedAt 上传时间
     * @return 补齐元数据后的文档
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
     * 判断文档是否包含有效文本内容。
     *
     * @param document 文档对象
     * @return 是否包含有效文本
     */
    private boolean hasTextContent(Document document) {
        return StringUtils.hasText(document.getText());
    }

    /**
     * 构建检索过滤条件。 默认仅检索当前模块数据；如果传入文件名，则进一步按文件名过滤。
     *
     * @param fileName 文件名
     * @return Spring AI 向量过滤表达式
     */
    private org.springframework.ai.vectorstore.filter.Filter.Expression buildSearchFilter(String fileName) {
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
     * 校验并返回最终的 topK。
     *
     * @param topK 入参 topK
     * @return 合法的 topK
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
     * 获取文件内容类型，缺失时返回默认值。
     *
     * @param file 上传文件
     * @return 文件内容类型
     */
    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_CONTENT_TYPE;
    }

    /**
     * 规范化必填文本参数。
     *
     * @param value        原始值
     * @param errorMessage 错误信息
     * @return 去除首尾空格后的值
     */
    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw VectorStoreException.badRequest(errorMessage);
        }
        return value.trim();
    }

    /**
     * 规范化可选文本参数。
     *
     * @param value 原始值
     * @return 去除首尾空格后的值，空白则返回 null
     */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 将 Spring AI 文档对象转换为接口响应对象。
     *
     * @param document 文档对象
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
}
