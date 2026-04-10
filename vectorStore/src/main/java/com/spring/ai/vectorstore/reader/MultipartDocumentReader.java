package com.spring.ai.vectorstore.reader;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart 文档读取策略接口。
 * 用于根据不同文件类型将上传文件解析为 Spring AI 文档对象。
 */
public interface MultipartDocumentReader {

    /**
     * 当前读取器是否支持指定扩展名。
     *
     * @param extension 文件扩展名
     * @return 是否支持
     */
    boolean supports(String extension);

    /**
     * 读取上传文件并转换为文档列表。
     *
     * @param file 上传文件
     * @return 文档列表
     */
    List<Document> read(MultipartFile file);
}
