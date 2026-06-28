package com.spring.ai.vectorstore.splitter;

import java.util.List;
import org.springframework.ai.document.Document;

/**
 * 向量文档切分服务接口。
 *
 * <p>该接口定义“源文档到入库 chunk”的转换入口，具体实现负责根据文件类型和 YAML 配置选择切分策略。</p>
 */
public interface VectorDocumentSplitter {

    /**
     * 按文件后缀和配置切分文档。
     *
     * @param documents 标准化后的源文档列表。每个 Document 应包含正文 text，以及模块名、文件名、后缀、上传时间等 metadata。
     * @param extension 文件后缀，例如 md、pdf、java、faq。实现类会根据后缀优先选择类型感知切分策略。
     * @return 切分后的文档列表。每个返回文档都是最终要写入向量库的 chunk，并携带 splitMode、chunkIndex 等切分元数据。
     */
    List<Document> split(List<Document> documents, String extension);
}