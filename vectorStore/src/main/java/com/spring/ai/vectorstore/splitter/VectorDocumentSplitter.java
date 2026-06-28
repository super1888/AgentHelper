package com.spring.ai.vectorstore.splitter;

import java.util.List;
import org.springframework.ai.document.Document;

/**
 * 向量文档切分服务，负责根据文件类型和配置生成最终入库 chunk。
 */
public interface VectorDocumentSplitter {

    /**
     * 按文件后缀和配置切分文档。
     * @param documents 标准化后的源文档
     * @param extension 文件后缀
     * @return 切分后的文档列表
     */
    List<Document> split(List<Document> documents, String extension);
}
