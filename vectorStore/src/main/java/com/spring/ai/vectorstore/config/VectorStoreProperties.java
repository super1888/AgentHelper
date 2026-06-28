package com.spring.ai.vectorstore.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库模块配置参数。
 *
 * <p>配置来源：</p>
 * <p>所有字段都绑定到 YAML 的 app.vector-store 前缀下，例如 app.vector-store.store-type。</p>
 *
 * <p>配置范围：</p>
 * <p>1. 存储后端：Redis、Qdrant、FAISS。</p>
 * <p>2. 文档切分：固定长度、递归、语义、文件类型感知切分。</p>
 * <p>3. 混合检索：向量检索、关键词检索、RRF 融合、Rerank 重排。</p>
 * <p>4. ANN 检索：HNSW、IVF、PQ 参数。</p>
 * <p>5. PDF 文本格式化、写入批次和并行写入策略。</p>
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app.vector-store")
public class VectorStoreProperties {

    /** 当前启用的向量库后端。REDIS 适合轻量原型，QDRANT 适合服务化 RAG，FAISS 适合本地离线索引。 */
    private StoreType storeType = StoreType.REDIS;
    /** 检索接口未传 topK 时使用的默认返回数量。 */
    private int defaultTopK = 3;
    /** 默认 chunk 大小，TokenTextSplitter、递归切分和语义切分都会参考该值。 */
    private int chunkSize = 800;
    /** 最小 chunk 字符数，用于避免产生过短、缺少语义的切片。 */
    private int minChunkSizeChars = 350;
    /** 最小嵌入文本长度，低于该值的片段通常不适合生成向量。 */
    private int minChunkLengthToEmbed = 5;
    /** 单个文档最大切片数量，防止超大文件产生过多向量写入请求。 */
    private int maxNumChunks = 128;
    /** 切分时是否保留分隔符，保留分隔符有助于保持句子、段落和代码结构边界。 */
    private boolean keepSeparator = true;
    /** PDF 文本抽取后是否做左对齐格式化。 */
    private boolean leftAlignment = false;
    /** PDF 抽取时跳过顶部页数，常用于删除封面、目录等无关内容。 */
    private int numberOfTopPagesToSkipBeforeDelete = 0;
    /** PDF 抽取时删除每页顶部文本行数，常用于去掉页眉。 */
    private int numberOfTopTextLinesToDelete = 0;
    /** PDF 抽取时删除每页底部文本行数，常用于去掉页脚和页码。 */
    private int numberOfBottomTextLinesToDelete = 0;
    /** 每次写入向量库的批次大小，过大可能触发嵌入模型限流或 token 限制。 */
    private int writeBatchSize = 8;
    /** 是否启用并行写入。大文件切片较多时可提升写入速度，但会增加模型和向量库压力。 */
    private boolean parallelWriteEnabled = false;
    /** 启用并行写入的切片数量阈值，低于该值时仍使用串行写入。 */
    private int parallelWriteThreshold = 64;
    /** 文档切分配置。 */
    private Split split = new Split();
    /** 混合检索配置。 */
    private HybridSearch hybridSearch = new HybridSearch();
    /** ANN 近似检索配置。 */
    private Ann ann = new Ann();
    /** Qdrant 连接配置。 */
    private Qdrant qdrant = new Qdrant();
    /** FAISS 本地索引配置。 */
    private Faiss faiss = new Faiss();

    public enum StoreType {
        /** 使用 Spring AI RedisVectorStore。 */
        REDIS,
        /** 使用 Spring AI QdrantVectorStore。 */
        QDRANT,
        /** 使用模块内本地 FAISS 风格索引。 */
        FAISS
    }

    public enum SplitMode {
        /** 固定长度切分，按 fixedLength 和 overlap 生成 chunk。 */
        FIXED,
        /** 递归切分，按 recursiveSeparators 从粗到细逐级切分。 */
        RECURSIVE,
        /** 语义切分，按句子聚合成语义相对完整的 chunk。 */
        SEMANTIC,
        /** 自动模式，优先使用文件类型感知切分，否则使用 Spring AI TokenTextSplitter。 */
        AUTO
    }

    public enum AnnAlgorithm {
        /** 图结构近似检索，适合高召回低延迟场景。 */
        HNSW,
        /** 倒排文件索引，适合大规模向量分桶检索。 */
        IVF,
        /** 乘积量化，适合压缩向量、降低内存占用的近似检索。 */
        PQ
    }

    @Setter
    @Getter
    public static class Split {
        /** 通用切分模式。文件类型感知切分优先级高于该模式。 */
        private SplitMode mode = SplitMode.AUTO;
        /** 固定长度切分时每个 chunk 的最大字符数。 */
        private int fixedLength = 800;
        /** 固定长度或兜底切分时，相邻 chunk 重叠字符数。 */
        private int overlap = 80;
        /** 递归切分分隔符，从左到右依次尝试，通常从段落到句子再到空格。 */
        private List<String> recursiveSeparators = new ArrayList<>(List.of("\n\n", "\n", "。", ".", " "));
        /** 语义切分窗口大小，预留给更复杂语义切分策略使用。 */
        private int semanticWindowSize = 3;
        /** 语义切分断点比例，值越高单个语义块越长。 */
        private double semanticBreakpointPercentile = 0.82D;
        /** Markdown 是否启用按标题层级切分。 */
        private boolean markdownHeadingEnabled = true;
        /** PDF 是否启用按自然段落切分。 */
        private boolean pdfParagraphEnabled = true;
        /** 代码文件是否启用按类、函数、方法边界切分。 */
        private boolean codeBlockEnabled = true;
        /** FAQ 文件是否启用按问答对切分。 */
        private boolean faqPairEnabled = true;

    }

    @Setter
    @Getter
    public static class HybridSearch {
        /** 是否启用混合检索。关闭后只走向量检索。 */
        private boolean enabled = true;
        /** 是否启用向量检索召回。 */
        private boolean vectorEnabled = true;
        /** 是否启用关键词检索召回。 */
        private boolean keywordEnabled = true;
        /** 关键词候选放大倍数，先多召回再交给 RRF 融合。 */
        private int keywordCandidateMultiplier = 4;
        /** 向量候选放大倍数，先多召回再交给 RRF 融合。 */
        private int vectorCandidateMultiplier = 4;
        /** RRF 平滑系数，值越大不同名次之间分差越小。 */
        private int rrfK = 60;
        /** 是否启用 Rerank 重排。 */
        private boolean rerankEnabled = true;
        /** 进入 Rerank 阶段的候选放大倍数。 */
        private int rerankCandidateMultiplier = 3;

    }

    @Setter
    @Getter
    public static class Ann {
        /** 是否启用 ANN 配置入口。 */
        private boolean enabled = true;
        /** 当前 ANN 算法。 */
        private AnnAlgorithm algorithm = AnnAlgorithm.HNSW;
        /** HNSW 每个节点最大连接数，值越大召回越好但内存占用越高。 */
        private int hnswM = 16;
        /** HNSW 建图阶段搜索宽度，值越大建图越慢但索引质量越好。 */
        private int hnswEfConstruction = 200;
        /** HNSW 查询阶段搜索宽度，值越大召回越好但查询越慢。 */
        private int hnswEfSearch = 64;
        /** IVF 聚类桶数量，桶越多索引越细但训练和维护成本越高。 */
        private int ivfNlist = 1024;
        /** IVF 查询时探测桶数量，值越大召回越好但查询越慢。 */
        private int ivfNprobe = 16;
        /** PQ 向量分段数量，分段越多压缩粒度越细。 */
        private int pqSegments = 8;
        /** PQ 每段量化位数，位数越高精度越高但压缩率越低。 */
        private int pqBits = 8;

    }

    @Setter
    @Getter
    public static class Qdrant {
        /** Qdrant 服务主机名。 */
        private String host = "localhost";
        /** Qdrant gRPC 端口，默认 6334。 */
        private int port = 6334;
        /** Qdrant collection 名称，对应一个向量集合。 */
        private String collectionName = "spring_ai_vector_store";
        /** Qdrant API Key，未开启认证时可为空。 */
        private String apiKey;
        /** 是否使用 TLS 连接 Qdrant。 */
        private boolean useTls = false;

    }

    @Setter
    @Getter
    public static class Faiss {
        /** 本地索引 JSON 文件路径，用于保存 FAISS 风格索引条目。 */
        private String indexPath = "data/faiss-vector-store.json";
        /** 是否对向量做单位化，开启后余弦相似度更稳定。 */
        private boolean normalizeVectors = true;

    }

}
