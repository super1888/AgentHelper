package com.spring.ai.vectorstore.reader;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Multipart 文档读取器注册表。
 * 负责根据文件扩展名找到对应的解析策略。
 */
@Component
public class MultipartDocumentReaderRegistry {

    private static final String[] SUPPORTED_EXTENSIONS = {
            "pdf", "txt", "md", "doc", "docx", "ppt", "pptx", "xls", "xlsx"
    };

    private final Map<String, MultipartDocumentReader> readersByExtension = new LinkedHashMap<>();

    public MultipartDocumentReaderRegistry(List<MultipartDocumentReader> readers) {
        for (MultipartDocumentReader reader : readers) {
            for (String extension : SUPPORTED_EXTENSIONS) {
                // 同一扩展名只保留首个匹配读取器，保证注册结果稳定。
                if (reader.supports(extension)) {
                    readersByExtension.putIfAbsent(extension, reader);
                }
            }
        }
    }

    /**
     * 根据扩展名获取读取器。
     *
     * @param extension 文件扩展名
     * @return 对应读取器
     */
    public MultipartDocumentReader getReader(String extension) {
        if (!StringUtils.hasText(extension)) {
            throw VectorStoreException.badRequest("File extension must not be blank");
        }
        MultipartDocumentReader reader = readersByExtension.get(extension.toLowerCase(Locale.ROOT));
        if (reader == null) {
            throw VectorStoreException.badRequest("Unsupported file extension: " + extension);
        }
        return reader;
    }
}
