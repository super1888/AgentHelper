# Spring AI 模块化 RAG 学习说明

## 1. 这次新增了什么

本次在 `core` 模块下新增了一套“模块化 RAG 学习组件”，位置如下：

- `core.rag.domain.dto.ModularRagRequest`
- `core.rag.domain.vo.ModularRagExecutionResult`
- `core.rag.postprocessor.SimpleKeywordDocumentPostProcessor`
- `core.rag.factory.ModularRagComponentFactory`
- `core.rag.service.ModularRagService`

另外在 `common` 中补充了：

- `common.constants.RagConstants`
- `common.enums.RagFlowTypeEnum`

## 2. 为什么要这么设计

你现在是学习 Spring AI Agent 和 RAG，所以最重要的不是一上来就把代码写得特别花，而是：

1. 每个阶段都能看懂
2. 每个组件都能单独替换
3. 能直接跑，也能拆开学

因此这套代码同时支持两种用法：

### 2.1 快速使用

直接创建：

- `QuestionAnswerAdvisor`
- `RetrievalAugmentationAdvisor`

适合先把功能跑起来。

### 2.2 手动执行

通过 `ModularRagService.executeModularRag(...)` 手动执行：

1. 查询转换
2. 查询扩展
3. 文档检索
4. 文档合并
5. 文档后处理
6. 查询增强

适合学习每一步在做什么。

## 3. 你给的 import 和当前官方包名的区别

你贴的示例思路是对的，但当前 Spring AI 1.1.x 的包名是“分层结构”，更准确的写法大致是：

### 3.1 Pre-Retrieval

- `org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer`
- `org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer`
- `org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer`
- `org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer`
- `org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander`
- `org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander`

### 3.2 Retrieval

- `org.springframework.ai.rag.retrieval.search.DocumentRetriever`
- `org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever`
- `org.springframework.ai.rag.retrieval.join.DocumentJoiner`
- `org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner`

### 3.3 Post-Retrieval

- `org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor`

### 3.4 Generation

- `org.springframework.ai.rag.generation.augmentation.QueryAugmenter`
- `org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter`

### 3.5 Advisor

- `org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor`
- `org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor`

## 4. 如何使用

### 4.1 直接创建 QuestionAnswerAdvisor

```java
@Resource
private ModularRagService modularRagService;

@Resource
private VectorStore vectorStore;

public QuestionAnswerAdvisor buildQaAdvisor() {
    ModularRagRequest request = ModularRagRequest.builder()
            .userQuery("什么是模块化 RAG")
            .topK(4)
            .similarityThreshold(0.5)
            .build();

    return modularRagService.createQuestionAnswerAdvisor(vectorStore, request);
}
```

### 4.2 创建 RetrievalAugmentationAdvisor

```java
public RetrievalAugmentationAdvisor buildRaAdvisor() {
    ModularRagRequest request = ModularRagRequest.builder()
            .userQuery("什么是模块化 RAG")
            .enableRewriteQuery(true)
            .enableCompressionQuery(true)
            .enableMultiQueryExpansion(true)
            .topK(4)
            .similarityThreshold(0.5)
            .build();

    return modularRagService.createRetrievalAugmentationAdvisor(vectorStore, request);
}
```

### 4.3 手动执行模块化 RAG

```java
public ModularRagExecutionResult runManualRag() {
    ModularRagRequest request = ModularRagRequest.builder()
            .userQuery("终端报文返回 dar=4 是什么意思")
            .enableRewriteQuery(true)
            .enableMultiQueryExpansion(true)
            .enableSimpleDocumentPostProcessor(true)
            .requiredKeywords(List.of("dar", "终端", "报文"))
            .topK(5)
            .similarityThreshold(0.5)
            .build();

    return modularRagService.executeModularRag(vectorStore, request);
}
```

## 5. 推荐学习顺序

建议你按这个顺序理解：

1. 先看 `QuestionAnswerAdvisor`
2. 再看 `RetrievalAugmentationAdvisor`
3. 再看 `executeModularRag(...)`
4. 最后自己替换 `DocumentPostProcessor`

## 6. 后续你可以继续扩展什么

你后面可以在这套结构上继续扩展：

1. 增加自定义 `DocumentPostProcessor`
2. 增加多个 `DocumentRetriever` 做混合检索
3. 增加重排序组件
4. 增加按业务场景切换不同 RAG 流程的工厂
5. 把 RAG 参数配置做成数据库或配置中心可动态调整
