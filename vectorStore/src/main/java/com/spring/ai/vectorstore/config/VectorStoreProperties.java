package com.spring.ai.vectorstore.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库模块配置参数，统一控制存储后端、切分策略、混合检索、重排和 ANN 索引策略。
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app.vector-store")
public class VectorStoreProperties {

    private StoreType storeType = StoreType.REDIS;
    private int defaultTopK = 3;
    private int chunkSize = 800;
    private int minChunkSizeChars = 350;
    private int minChunkLengthToEmbed = 5;
    private int maxNumChunks = 128;
    private boolean keepSeparator = true;
    private boolean leftAlignment = false;
    private int numberOfTopPagesToSkipBeforeDelete = 0;
    private int numberOfTopTextLinesToDelete = 0;
    private int numberOfBottomTextLinesToDelete = 0;
    private int writeBatchSize = 8;
    private boolean parallelWriteEnabled = false;
    private int parallelWriteThreshold = 64;
    private Split split = new Split();
    private HybridSearch hybridSearch = new HybridSearch();
    private Ann ann = new Ann();
    private Qdrant qdrant = new Qdrant();
    private Faiss faiss = new Faiss();

    public enum StoreType {
        REDIS,
        QDRANT,
        FAISS
    }

    public enum SplitMode {
        FIXED,
        RECURSIVE,
        SEMANTIC,
        AUTO
    }

    public enum AnnAlgorithm {
        HNSW,
        IVF,
        PQ
    }

    @Setter
    @Getter
    public static class Split {
        private SplitMode mode = SplitMode.AUTO;
        private int fixedLength = 800;
        private int overlap = 80;
        private List<String> recursiveSeparators = new ArrayList<>(List.of("\n\n", "\n", "。", ".", " "));
        private int semanticWindowSize = 3;
        private double semanticBreakpointPercentile = 0.82D;
        private boolean markdownHeadingEnabled = true;
        private boolean pdfParagraphEnabled = true;
        private boolean codeBlockEnabled = true;
        private boolean faqPairEnabled = true;

    }

    @Setter
    @Getter
    public static class HybridSearch {
        private boolean enabled = true;
        private boolean vectorEnabled = true;
        private boolean keywordEnabled = true;
        private int keywordCandidateMultiplier = 4;
        private int vectorCandidateMultiplier = 4;
        private int rrfK = 60;
        private boolean rerankEnabled = true;
        private int rerankCandidateMultiplier = 3;

    }

    @Setter
    @Getter
    public static class Ann {
        private boolean enabled = true;
        private AnnAlgorithm algorithm = AnnAlgorithm.HNSW;
        private int hnswM = 16;
        private int hnswEfConstruction = 200;
        private int hnswEfSearch = 64;
        private int ivfNlist = 1024;
        private int ivfNprobe = 16;
        private int pqSegments = 8;
        private int pqBits = 8;

    }

    @Setter
    @Getter
    public static class Qdrant {
        private String host = "localhost";
        private int port = 6334;
        private String collectionName = "spring_ai_vector_store";
        private String apiKey;
        private boolean useTls = false;

    }

    @Setter
    @Getter
    public static class Faiss {
        private String indexPath = "data/faiss-vector-store.json";
        private boolean normalizeVectors = true;

    }

}
