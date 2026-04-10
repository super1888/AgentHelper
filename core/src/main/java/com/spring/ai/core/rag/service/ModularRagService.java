package com.spring.ai.core.rag.service;

import com.spring.ai.common.constants.RagConstants;
import com.spring.ai.common.enums.RagFlowTypeEnum;
import com.spring.ai.core.rag.domain.dto.ModularRagRequest;
import com.spring.ai.core.rag.domain.vo.ModularRagExecutionResult;
import com.spring.ai.core.rag.factory.ModularRagComponentFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模块化 RAG 服务。
 *
 * <p>这个服务的目标是把 Spring AI 模块化 RAG 的完整流程拆开给你看清楚。
 * 你可以把它理解成一个“教学型 RAG 门面”： 1. 既能返回 Advisor，直接给 ChatClient 用 2. 也能手动执行每个 RAG 阶段，查看每一步结果</p>
 */
@Service
public class ModularRagService {

    private final ModularRagComponentFactory componentFactory;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public ModularRagService(
            ModularRagComponentFactory componentFactory,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.componentFactory = componentFactory;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    /**
     * 创建 QuestionAnswerAdvisor。
     *
     * @param vectorStore 向量库
     * @param request     请求参数
     * @return QuestionAnswerAdvisor
     */
    public QuestionAnswerAdvisor createQuestionAnswerAdvisor(VectorStore vectorStore, ModularRagRequest request) {
        return componentFactory.createQuestionAnswerAdvisor(vectorStore, normalizeRequest(request));
    }

    /**
     * 创建模块化 RetrievalAugmentationAdvisor。
     *
     * @param vectorStore 向量库
     * @param request     请求参数
     * @return RetrievalAugmentationAdvisor
     */
    public RetrievalAugmentationAdvisor createRetrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ModularRagRequest request) {
        return componentFactory.createRetrievalAugmentationAdvisor(
                vectorStore,
                requireChatClientBuilder(),
                normalizeRequest(request));
    }

    /**
     * 手动执行一次模块化 RAG。
     *
     * <p>这个方法适合学习：它会显式执行
     * 查询转换 -> 查询扩展 -> 文档检索 -> 文档合并 -> 文档后处理 -> 查询增强 并把中间结果全部返回。</p>
     *
     * @param vectorStore 向量库
     * @param request     请求参数
     * @return 模块化 RAG 执行结果
     */
    public ModularRagExecutionResult executeModularRag(VectorStore vectorStore, ModularRagRequest request) {
        ModularRagRequest normalizedRequest = normalizeRequest(request);
        ChatClient.Builder chatClientBuilder = requireChatClientBuilder();

        Query originalQuery = new Query(normalizedRequest.getUserQuery());
        Query transformedQuery = applyQueryTransformers(originalQuery, chatClientBuilder, normalizedRequest);
        List<Query> expandedQueries = applyQueryExpander(transformedQuery, chatClientBuilder, normalizedRequest);

        DocumentRetriever retriever = componentFactory.createVectorStoreDocumentRetriever(vectorStore, normalizedRequest);
        DocumentJoiner documentJoiner = componentFactory.createDocumentJoiner();
        Map<Query, List<List<Document>>> documentsForQuery = new LinkedHashMap<>();

        for (Query expandedQuery : expandedQueries) {
            List<Document> retrievedDocuments = retriever.retrieve(expandedQuery);
            documentsForQuery.put(expandedQuery, List.of(retrievedDocuments));
        }

        List<Document> joinedDocuments = documentJoiner.join(documentsForQuery);
        List<Document> processedDocuments = applyDocumentPostProcessors(joinedDocuments, transformedQuery, normalizedRequest);

        QueryAugmenter queryAugmenter = componentFactory.createQueryAugmenter(normalizedRequest);
        Query augmentedQuery = queryAugmenter.augment(transformedQuery, processedDocuments);

        return ModularRagExecutionResult.builder()
                .originalQuery(originalQuery.text())
                .transformedQuery(transformedQuery.text())
                .expandedQueries(expandedQueries.stream().map(Query::text).toList())
                .documents(processedDocuments)
                .augmentedQuery(augmentedQuery.text())
                .build();
    }

    private Query applyQueryTransformers(
            Query query,
            ChatClient.Builder chatClientBuilder,
            ModularRagRequest request) {
        List<QueryTransformer> transformers = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getEnableRewriteQuery())) {
            transformers.add(componentFactory.createRewriteQueryTransformer(chatClientBuilder, request.getTargetSearchSystem()));
        }
        if (Boolean.TRUE.equals(request.getEnableCompressionQuery())) {
            transformers.add(componentFactory.createCompressionQueryTransformer(chatClientBuilder));
        }
        if (Boolean.TRUE.equals(request.getEnableTranslationQuery())) {
            transformers.add(componentFactory.createTranslationQueryTransformer(chatClientBuilder, request.getTargetLanguage()));
        }

        Query currentQuery = query;
        for (QueryTransformer transformer : transformers) {
            currentQuery = transformer.transform(currentQuery);
        }
        return currentQuery;
    }

    private List<Query> applyQueryExpander(
            Query query,
            ChatClient.Builder chatClientBuilder,
            ModularRagRequest request) {
        if (!Boolean.TRUE.equals(request.getEnableMultiQueryExpansion())) {
            return List.of(query);
        }
        QueryExpander queryExpander = componentFactory.createMultiQueryExpander(
                chatClientBuilder,
                request.getExpandedQueryCount());
        return queryExpander.expand(query);
    }

    private List<Document> applyDocumentPostProcessors(
            List<Document> documents,
            Query query,
            ModularRagRequest request) {
        List<Document> currentDocuments = documents;
        List<DocumentPostProcessor> processors = componentFactory.createDocumentPostProcessors(request);
        for (DocumentPostProcessor processor : processors) {
            currentDocuments = processor.process(query, currentDocuments);
        }
        return currentDocuments;
    }

    private ChatClient.Builder requireChatClientBuilder() {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("当前容器中没有 ChatClient.Builder Bean，请确认 Spring AI 自动配置是否生效");
        }
        return builder;
    }

    private ModularRagRequest normalizeRequest(ModularRagRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ModularRagRequest 不能为空");
        }
        if (!StringUtils.hasText(request.getUserQuery())) {
            throw new IllegalArgumentException("userQuery 不能为空");
        }

        return ModularRagRequest.builder()
                .userQuery(request.getUserQuery().trim())
                .ragFlowType(request.getRagFlowType() != null ? request.getRagFlowType() : RagFlowTypeEnum.MANUAL_MODULAR_RAG)
                .enableRewriteQuery(Boolean.TRUE.equals(request.getEnableRewriteQuery()))
                .enableCompressionQuery(Boolean.TRUE.equals(request.getEnableCompressionQuery()))
                .enableTranslationQuery(Boolean.TRUE.equals(request.getEnableTranslationQuery()))
                .targetLanguage(StringUtils.hasText(request.getTargetLanguage()) ? request.getTargetLanguage() : RagConstants.DEFAULT_TARGET_LANGUAGE)
                .targetSearchSystem(StringUtils.hasText(request.getTargetSearchSystem()) ? request.getTargetSearchSystem()
                        : RagConstants.DEFAULT_TARGET_SEARCH_SYSTEM)
                .enableMultiQueryExpansion(Boolean.TRUE.equals(request.getEnableMultiQueryExpansion()))
                .expandedQueryCount(
                        request.getExpandedQueryCount() != null ? request.getExpandedQueryCount() : RagConstants.DEFAULT_EXPANDED_QUERY_COUNT)
                .topK(request.getTopK() != null ? request.getTopK() : RagConstants.DEFAULT_TOP_K)
                .similarityThreshold(request.getSimilarityThreshold() != null
                        ? request.getSimilarityThreshold()
                        : RagConstants.DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(request.getFilterExpression())
                .allowEmptyContext(Boolean.TRUE.equals(request.getAllowEmptyContext()))
                .enableSimpleDocumentPostProcessor(Boolean.TRUE.equals(request.getEnableSimpleDocumentPostProcessor()))
                .postProcessorMaxChars(request.getPostProcessorMaxChars() != null
                        ? request.getPostProcessorMaxChars()
                        : RagConstants.DEFAULT_POST_PROCESSOR_MAX_CHARS)
                .requiredKeywords(request.getRequiredKeywords())
                .build();
    }
}
