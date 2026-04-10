package com.spring.ai.vectorstore.manager;

import com.spring.ai.common.utils.FileReaderUtil;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 向量存储manager
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
@Component
public class VectorStoreManager {


    @Resource
    private VectorStore vectorStore;

    private final TokenTextSplitter splitter = new TokenTextSplitter();

    // ====================== 上传文件（携带文件名） ======================
    public void upload(MultipartFile file) throws Exception {
        String text = FileReaderUtil.readFile(file);
        String fileName = file.getOriginalFilename();

        List<Document> documents = splitter.apply(List.of(
                new Document(text, Map.of("fileName", fileName)) // 存入文件名
        ));

        vectorStore.add(documents);
    }

    // ====================== 搜索 ======================
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build());
    }

    // ====================== 删除全部向量 ======================
    public void deleteAll() {
        vectorStore.delete(List.of("*"));
    }

    // ====================== 根据文件名删除 ======================
    public void deleteByFileName(String fileName) {
        // 1. 查所有向量
        List<Document> allDocs = vectorStore.similaritySearch("");

        // 2. 过滤出当前文件名的向量
        List<Document> targetDocs = allDocs.stream()
                .filter(doc -> fileName.equals(doc.getMetadata().get("fileName")))
                .toList();

        // 3. 删除
        if (!targetDocs.isEmpty()) {
            List<String> ids = targetDocs.stream()
                    .map(Document::getId)
                    .toList();
            vectorStore.delete(ids);
        }
    }
}
