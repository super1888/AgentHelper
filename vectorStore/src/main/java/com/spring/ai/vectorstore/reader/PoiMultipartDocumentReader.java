package com.spring.ai.vectorstore.reader;

import com.alibaba.cloud.ai.reader.poi.PoiDocumentReader;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Office 文档读取器。
 * 基于阿里云 POI 文档读取器解析 Word、PPT、Excel 等 Office 文件。
 */
@Component
public class PoiMultipartDocumentReader implements MultipartDocumentReader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("doc", "docx", "ppt", "pptx", "xls", "xlsx");

    private final ExtractedTextFormatter extractedTextFormatter;

    public PoiMultipartDocumentReader(ExtractedTextFormatter extractedTextFormatter) {
        this.extractedTextFormatter = extractedTextFormatter;
    }

    @Override
    public boolean supports(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * 读取 Office 文件并提取文本。
     *
     * @param file 上传文件
     * @return 文档列表
     */
    @Override
    public List<Document> read(MultipartFile file) {
        try {
            PoiDocumentReader poiDocumentReader = new PoiDocumentReader(
                    new MultipartFileResource(file.getBytes(), file.getOriginalFilename()),
                    extractedTextFormatter);
            return poiDocumentReader.get();
        }
        catch (IOException exception) {
            throw VectorStoreException.internalError("Failed to parse Office file: " + file.getOriginalFilename(), exception);
        }
    }
}
