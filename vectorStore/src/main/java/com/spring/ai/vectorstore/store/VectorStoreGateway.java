package com.spring.ai.vectorstore.store;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

/**
 * 向量存储统一网关。
 *
 * <p>核心作用：</p>
 * <p>1. 上层业务只关心“写入、检索、删除、枚举文档”这些通用动作。</p>
 * <p>2. Redis、Qdrant、FAISS 的客户端、索引结构、删除方式和枚举能力不同，差异全部收敛到该接口的实现类中。</p>
 * <p>3. 服务层通过该接口调用向量库，后续切换存储后端时不需要改 Controller 和主业务流程。</p>
 */
public interface VectorStoreGateway {

    /**
     * 检查当前向量库是否可用。
     *
     * <p>处理含义：</p>
     * <p>1. Redis 模式会检查 Redis Stack、RedisJSON、RediSearch 等能力是否可用。</p>
     * <p>2. Qdrant 模式主要依赖 Spring AI 自动配置创建客户端，网关层不额外做连接探测。</p>
     * <p>3. FAISS 本地模式会检查 EmbeddingModel 是否存在，因为本地索引写入和检索都需要先生成向量。</p>
     */
    void ensureReady();

    /**
     * 批量写入文档到向量库。
     *
     * @param documents 待写入的文档切片列表。每个 Document 通常包含文本内容、文件名、模块名、上传时间、切分模式等元数据。
     *
     * <p>处理含义：</p>
     * <p>1. 上游已经完成文件解析、元数据补充和文本切分，这里只负责持久化到对应向量库。</p>
     * <p>2. Redis 和 Qdrant 会走 Spring AI VectorStore，由底层自动调用 EmbeddingModel 生成向量并写入。</p>
     * <p>3. FAISS 本地模式会在实现类中生成向量、保存内存索引，并同步写入本地索引文件。</p>
     */
    void add(List<Document> documents);

    /**
     * 执行向量相似度检索。
     *
     * @param searchRequest 检索请求。query 表示用户问题，topK 表示候选数量，similarityThreshold 表示最低相似度，filterExpression 表示模块或文件过滤条件。
     * @return 按向量相似度排序后的命中文档列表。
     *
     * <p>处理含义：</p>
     * <p>1. 将用户问题转换成向量后，在向量库中查找语义最接近的文档切片。</p>
     * <p>2. 返回结果会作为混合检索中“向量检索”这一条召回链路的候选集。</p>
     */
    List<Document> similaritySearch(SearchRequest searchRequest);

    /**
     * 删除满足过滤条件的向量文档。
     *
     * @param expression Spring AI 过滤表达式，通常包含模块名过滤，也可能包含文件名过滤。
     *
     * <p>处理含义：</p>
     * <p>1. 清空模块数据时只传模块过滤条件。</p>
     * <p>2. 按文件删除时会同时传模块名和文件名过滤条件。</p>
     * <p>3. 各后端根据自身能力完成删除，FAISS 本地模式会同步更新本地索引文件。</p>
     */
    void delete(Expression expression);

    /**
     * 枚举当前向量库中的文档切片。
     *
     * @param fileName 文件名过滤条件。为空时表示列出当前模块的全部文档；非空时只返回指定文件的切片。
     * @return 满足过滤条件的文档切片列表。
     *
     * <p>处理含义：</p>
     * <p>1. 关键词检索需要先拿到候选文本，再计算词项命中分数，因此需要文档枚举能力。</p>
     * <p>2. Redis 模式通过扫描 Redis JSON 文档实现枚举。</p>
     * <p>3. Qdrant 模式默认使用运行期写入镜像作为关键词检索候选，服务重启后如果没有镜像，会自动降级为纯向量召回。</p>
     * <p>4. FAISS 本地模式直接从本地索引条目中枚举。</p>
     */
    List<Document> listDocuments(String fileName);
}