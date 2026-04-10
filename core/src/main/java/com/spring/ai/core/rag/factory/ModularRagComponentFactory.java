package com.spring.ai.core.rag.factory;

import com.spring.ai.common.constants.RagConstants;
import com.spring.ai.core.rag.domain.dto.ModularRagRequest;
import com.spring.ai.core.rag.postprocessor.SimpleKeywordDocumentPostProcessor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 模块化 RAG 组件工厂。
 *
 * <p>这个工厂的职责不是直接回答问题，而是负责把 Spring AI 官方提供的
 * RAG 组件组装起来，方便你按模块理解和复用：
 * 1. Pre-Retrieval：QueryTransformer、QueryExpander
 * 2. Retrieval：DocumentRetriever、DocumentJoiner
 * 3. Post-Retrieval：DocumentPostProcessor
 * 4. Generation：QueryAugmenter
 * 5. Advisor：QuestionAnswerAdvisor、RetrievalAugmentationAdvisor</p>
 */
@Component
public class ModularRagComponentFactory {

    /**
     * 创建查询重写组件。
     *
     * <p>查询重写的作用是把用户自然语言问题改写成更适合检索系统的表达方式。</p>
     *
     * @param chatClientBuilder ChatClient 构建器
     * @param targetSearchSystem 目标检索系统描述，例如 vector store
     * @return 查询重写组件
     */
    public QueryTransformer createRewriteQueryTransformer(
            ChatClient.Builder chatClientBuilder,
            String targetSearchSystem) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .targetSearchSystem(StringUtils.hasText(targetSearchSystem)
                        ? targetSearchSystem
                        : RagConstants.DEFAULT_TARGET_SEARCH_SYSTEM)
                .build();
    }

    /**
     * 创建查询压缩组件。
     *
     * <p>查询压缩适合把用户冗长问题压缩成更短、更聚焦的检索词。</p>
     *
     * @param chatClientBuilder ChatClient 构建器
     * @return 查询压缩组件
     */
    public QueryTransformer createCompressionQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .build();
    }

    /**
     * 创建查询翻译组件。
     *
     * <p>如果向量库主要使用英文 embedding，可以先把中文问题翻译成英文再检索。</p>
     *
     * @param chatClientBuilder ChatClient 构建器
     * @param targetLanguage 目标语言
     * @return 查询翻译组件
     */
    public QueryTransformer createTranslationQueryTransformer(
            ChatClient.Builder chatClientBuilder,
            String targetLanguage) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .targetLanguage(StringUtils.hasText(targetLanguage)
                        ? targetLanguage
                        : RagConstants.DEFAULT_TARGET_LANGUAGE)
                .build();
    }

    /**
     * 创建多查询扩展组件。
     *
     * <p>该组件会根据一个原始问题扩展出多个相近但不同角度的查询，以提升召回率。</p>
     *
     * @param chatClientBuilder ChatClient 构建器
     * @param numberOfQueries 扩展查询数量
     * @return 多查询扩展器
     */
    public QueryExpander createMultiQueryExpander(ChatClient.Builder chatClientBuilder, Integer numberOfQueries) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .includeOriginal(Boolean.TRUE)
                .numberOfQueries(numberOfQueries != null ? numberOfQueries : RagConstants.DEFAULT_EXPANDED_QUERY_COUNT)
                .build();
    }

    /**
     * 创建向量检索器。
     *
     * <p>该组件负责真正去 VectorStore 中查找语义相近文档。</p>
     *
     * @param vectorStore 向量库
     * @param request RAG 请求参数
     * @return 文档检索器
     */
    public DocumentRetriever createVectorStoreDocumentRetriever(VectorStore vectorStore, ModularRagRequest request) {
        VectorStoreDocumentRetriever.Builder builder = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(request.getTopK() != null ? request.getTopK() : RagConstants.DEFAULT_TOP_K)
                .similarityThreshold(request.getSimilarityThreshold() != null
                        ? request.getSimilarityThreshold()
                        : RagConstants.DEFAULT_SIMILARITY_THRESHOLD);

        if (request.getFilterExpression() != null) {
            builder.filterExpression(request.getFilterExpression());
        }
        return builder.build();
    }

    /**
     * 创建文档合并器。
     *
     * <p>当一个问题被扩展成多个查询后，每个查询都会召回一组文档。
     * 文档合并器负责把这些文档合并成统一结果集。</p>
     *
     * @return 文档合并器
     */
    public DocumentJoiner createDocumentJoiner() {
        return new ConcatenationDocumentJoiner();
    }

    /**
     * 创建文档后处理器列表。
     *
     * <p>这里先提供一个简单实现，便于你理解 Post-Retrieval 阶段是怎么工作的。</p>
     *
     * @param request RAG 请求参数
     * @return 文档后处理器列表
     */
    public List<DocumentPostProcessor> createDocumentPostProcessors(ModularRagRequest request) {
        List<DocumentPostProcessor> processors = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getEnableSimpleDocumentPostProcessor())) {
            processors.add(new SimpleKeywordDocumentPostProcessor(
                    request.getRequiredKeywords(),
                    request.getPostProcessorMaxChars() != null
                            ? request.getPostProcessorMaxChars()
                            : RagConstants.DEFAULT_POST_PROCESSOR_MAX_CHARS));
        }
        return processors;
    }

    /**
     * 创建查询增强器。
     *
     * <p>该组件会把召回到的上下文拼接回 Query，最终交给大模型生成答案。</p>
     *
     * @param request RAG 请求参数
     * @return 查询增强器
     */
    public QueryAugmenter createQueryAugmenter(ModularRagRequest request) {
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(Boolean.TRUE.equals(request.getAllowEmptyContext()))
                .build();
    }

    /**
     * 创建开箱即用的 QuestionAnswerAdvisor。
     *
     * <p>这是最适合快速接入的 Advisor，适合初学阶段快速体验 RAG。</p>
     *
     * @param vectorStore 向量库
     * @param request RAG 请求参数
     * @return QuestionAnswerAdvisor
     */
    public QuestionAnswerAdvisor createQuestionAnswerAdvisor(VectorStore vectorStore, ModularRagRequest request) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .topK(request.getTopK() != null ? request.getTopK() : RagConstants.DEFAULT_TOP_K)
                .similarityThreshold(request.getSimilarityThreshold() != null
                        ? request.getSimilarityThreshold()
                        : RagConstants.DEFAULT_SIMILARITY_THRESHOLD);

        if (request.getFilterExpression() != null) {
            searchRequestBuilder.filterExpression(request.getFilterExpression());
        }

        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequestBuilder.build())
                .build();
    }

    /**
     * 创建模块化 RetrievalAugmentationAdvisor。
     *
     * <p>该 Advisor 比 QuestionAnswerAdvisor 更灵活，适合你后续自己组合各个 RAG 模块。</p>
     *
     * @param vectorStore 向量库
     * @param chatClientBuilder ChatClient 构建器
     * @param request RAG 请求参数
     * @return RetrievalAugmentationAdvisor
     */
    public RetrievalAugmentationAdvisor createRetrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            ModularRagRequest request) {

        List<QueryTransformer> queryTransformers = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getEnableRewriteQuery())) {
            queryTransformers.add(createRewriteQueryTransformer(chatClientBuilder, request.getTargetSearchSystem()));
        }
        if (Boolean.TRUE.equals(request.getEnableCompressionQuery())) {
            queryTransformers.add(createCompressionQueryTransformer(chatClientBuilder));
        }
        if (Boolean.TRUE.equals(request.getEnableTranslationQuery())) {
            queryTransformers.add(createTranslationQueryTransformer(chatClientBuilder, request.getTargetLanguage()));
        }

        RetrievalAugmentationAdvisor.Builder builder = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(createVectorStoreDocumentRetriever(vectorStore, request))
                .documentJoiner(createDocumentJoiner())
                .queryAugmenter(createQueryAugmenter(request));

        if (!queryTransformers.isEmpty()) {
            builder.queryTransformers(queryTransformers);
        }
        if (Boolean.TRUE.equals(request.getEnableMultiQueryExpansion())) {
            builder.queryExpander(createMultiQueryExpander(chatClientBuilder, request.getExpandedQueryCount()));
        }

        List<DocumentPostProcessor> processors = createDocumentPostProcessors(request);
        if (!processors.isEmpty()) {
            builder.documentPostProcessors(processors);
        }

        return builder.build();
    }

    /**
     * 创建一个简单的 PromptTemplate，方便后续自定义 QueryTransformer 或 QueryExpander 提示词。
     *
     * @param template 模板文本
     * @return PromptTemplate
     */
    public PromptTemplate createPromptTemplate(String template) {
        return new PromptTemplate(template);
    }
}
