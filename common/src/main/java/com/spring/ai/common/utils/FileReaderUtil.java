package com.spring.ai.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件读取工具
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
public class FileReaderUtil {
    public static String readFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();

        try (InputStream in = file.getInputStream()) {
            if (fileName.endsWith(".pdf")) {
                return readPdf(in);
            } else if (fileName.endsWith(".docx")) {
                return readWord(in);
            } else if (fileName.endsWith(".txt")) {
                return new String(in.readAllBytes());
            } else {
                throw new RuntimeException("不支持的文件类型");
            }
        }
    }

    private static String readPdf(InputStream in) throws Exception {
        byte[] bytes = in.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static String readWord(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            return new XWPFWordExtractor(doc).getText();
        }
    }
}
