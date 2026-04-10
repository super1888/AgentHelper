package com.spring.ai.vectorstore.reader;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文本文件读取器。
 * 支持 txt 与 md 文件解析。
 */
@Component
public class TextMultipartDocumentReader implements MultipartDocumentReader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md");

    @Resource
    private ExtractedTextFormatter extractedTextFormatter;

    @Override
    public boolean supports(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * 读取文本文件并提取内容。
     *
     * @param file 上传文件
     * @return 文档列表
     */
    @Override
    public List<Document> read(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return List.of(new Document(extractedTextFormatter.format(text)));
        }
        catch (IOException exception) {
            throw VectorStoreException.internalError("Failed to parse text file: " + file.getOriginalFilename(), exception);
        }
    }
}
