package com.spring.ai.vectorstore.reader;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF 文档读取器。
 * 基于 PDFBox 提取文本内容，并使用统一文本格式化器做预处理。
 */
@Component
public class PdfMultipartDocumentReader implements MultipartDocumentReader {

    @Resource
    private ExtractedTextFormatter extractedTextFormatter;

    @Override
    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    /**
     * 读取 PDF 文件并提取文本。
     *
     * @param file 上传文件
     * @return 文档列表
     */
    @Override
    public List<Document> read(MultipartFile file) {
        try (PDDocument pdfDocument = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(pdfDocument);
            return List.of(new Document(extractedTextFormatter.format(text)));
        }
        catch (IOException exception) {
            throw VectorStoreException.internalError("Failed to parse PDF file: " + file.getOriginalFilename(), exception);
        }
    }
}
