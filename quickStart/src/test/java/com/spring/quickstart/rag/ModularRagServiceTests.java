package com.spring.quickstart.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.spring.ai.common.constants.VectorStoreManagerConstants;
import com.spring.ai.common.enums.RagFlowTypeEnum;
import com.spring.ai.core.rag.domain.dto.ModularRagRequest;
import com.spring.ai.core.rag.domain.vo.ModularRagExecutionResult;
import com.spring.ai.core.rag.service.ModularRagService;
import com.spring.quickstart.QuickStartApplication;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 模块化 RAG 测试类。
 *
 * <p>这个测试类单独抽出来，不再继续堆到 QuickStartApplicationTests 里，
 * 目的是让 RAG 的学习示例更聚焦、更容易理解。</p>
 *
 * <p>说明：
 * 1. 前两个测试主要验证组件是否能正确创建 2. 后面的手动流程测试依赖本地 Redis 向量库、Embedding 模型和聊天模型 3. 如果你本地还没有准备好向量数据，可以先运行前两个测试</p>
 */
@SpringBootTest(classes = QuickStartApplication.class)
class ModularRagServiceTests {

    @Resource
    private ModularRagService modularRagService;

    @Resource
    private VectorStore vectorStore;

    /**
     * 验证基础版 QuestionAnswerAdvisor 是否可以正常创建。
     *
     * <p>这个测试不会真正发起大模型回答，只验证：
     * 1. Spring 容器中的 ModularRagService 是否可用 2. VectorStore 是否已经注入 3. QuestionAnswerAdvisor 能否按请求参数正确组装</p>
     */
    @Test
    @DisplayName("应该可以创建 QuestionAnswerAdvisor")
    void shouldCreateQuestionAnswerAdvisor() {
        ModularRagRequest request = ModularRagRequest.builder()

                .userQuery("什么是 RAG")
                .ragFlowType(RagFlowTypeEnum.QUESTION_ANSWER_ADVISOR)
                .topK(3)
                .similarityThreshold(0.50D)
                .filterExpression(new FilterExpressionBuilder()
                        .eq(VectorStoreManagerConstants.METADATA_MODULE, VectorStoreManagerConstants.MODULE_NAME)
                        .build())
                .build();

        QuestionAnswerAdvisor advisor = modularRagService.createQuestionAnswerAdvisor(vectorStore, request);

        assertNotNull(modularRagService);
        assertNotNull(vectorStore);
        assertNotNull(advisor);
    }

    /**
     * 验证模块化 RetrievalAugmentationAdvisor 是否可以正常创建。
     *
     * <p>该测试用于确认：
     * 1. 查询重写、压缩、扩展等模块可以被工厂正确组装 2. RetrievalAugmentationAdvisor 这条模块化流程能够正常构建</p>
     */
    @Test
    @DisplayName("应该可以创建 RetrievalAugmentationAdvisor")
    void shouldCreateRetrievalAugmentationAdvisor() {
        ModularRagRequest request = ModularRagRequest.builder()
                .userQuery("终端报文 dar=4 是什么意思")
                .ragFlowType(RagFlowTypeEnum.RETRIEVAL_AUGMENTATION_ADVISOR)
                .enableRewriteQuery(true)
                .enableCompressionQuery(true)
                .enableMultiQueryExpansion(true)
                .topK(4)
                .similarityThreshold(0.55D)
                .allowEmptyContext(true)
                .build();

        RetrievalAugmentationAdvisor advisor =
                modularRagService.createRetrievalAugmentationAdvisor(vectorStore, request);

        assertNotNull(advisor);
    }

    /**
     * 手动执行一次模块化 RAG。
     *
     * <p>这个测试依赖外部环境：
     * 1. 本地 Redis VectorStore 必须可用 2. 向量库中最好已经提前写入过文档 3. DashScope / 其他模型配置必须可用
     * <p>
     * 如果你本地环境还没完全准备好，可以先保持禁用，等向量数据准备好后再打开。</p>
     */
    @Test
    @Disabled("需要本地 Redis 向量库、Embedding 模型和已入库文档数据后再启用")
    @DisplayName("应该可以手动执行模块化 RAG 全流程")
    void shouldExecuteManualModularRagFlow() {
        ModularRagRequest request = ModularRagRequest.builder()
                .userQuery("终端报文返回 dar=4 是什么意思")
                .ragFlowType(RagFlowTypeEnum.MANUAL_MODULAR_RAG)
                .enableRewriteQuery(true)
                .enableCompressionQuery(true)
                .enableMultiQueryExpansion(true)
                .enableSimpleDocumentPostProcessor(true)
                .requiredKeywords(List.of("dar", "终端", "报文"))
                .topK(5)
                .similarityThreshold(0.50D)
                .allowEmptyContext(true)
                .filterExpression(new FilterExpressionBuilder()
                        .eq(VectorStoreManagerConstants.METADATA_MODULE, VectorStoreManagerConstants.MODULE_NAME)
                        .build())
                .build();

        ModularRagExecutionResult result = modularRagService.executeModularRag(vectorStore, request);

        assertNotNull(result);
        assertNotNull(result.getOriginalQuery());
        assertNotNull(result.getTransformedQuery());
        assertNotNull(result.getExpandedQueries());
        assertNotNull(result.getDocuments());
        assertNotNull(result.getAugmentedQuery());
        assertFalse(result.getOriginalQuery().isBlank());
        assertFalse(result.getAugmentedQuery().isBlank());
    }

    /**
     * 验证手动流程下即使检索不到文档，流程结构也应当完整。
     *
     * <p>这个测试也是一个学习型示例，帮助你观察：
     * 1. transformedQuery 是否生成 2. expandedQueries 是否生成 3. documents 字段是否被完整返回</p>
     */
    @Test
    @Disabled("依赖实际模型与向量库环境，建议本地数据准备好后再启用")
    @DisplayName("手动模块化 RAG 的执行结果应包含完整阶段信息")
    void shouldContainStageInformationInManualRagResult() {
        ModularRagRequest request = ModularRagRequest.builder()
                .userQuery("什么是应用连接的数据交换")
                .ragFlowType(RagFlowTypeEnum.MANUAL_MODULAR_RAG)
                .enableRewriteQuery(true)
                .enableMultiQueryExpansion(true)
                .allowEmptyContext(true)
                .topK(3)
                .similarityThreshold(0.40D)
                .build();

        ModularRagExecutionResult result = modularRagService.executeModularRag(vectorStore, request);

        assertNotNull(result.getOriginalQuery());
        assertNotNull(result.getTransformedQuery());
        assertNotNull(result.getExpandedQueries());
        assertFalse(result.getExpandedQueries().isEmpty());
        assertNotNull(result.getDocuments());
        assertNotNull(result.getAugmentedQuery());
    }
}
