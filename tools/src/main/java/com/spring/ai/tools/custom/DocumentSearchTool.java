package com.spring.ai.tools.custom;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.common.constants.VectorStoreManagerConstants;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.StringUtils;

/**
 * Vector-store backed document search tool.
 *
 * <p>The tool uses constructor injection so it can be created with an existing
 * VectorStore instance and registered into the agent as a normal method tool.</p>
 */
public class DocumentSearchTool {

    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_DOC_CHARS = 800;

    private final VectorStore vectorStore;

    public DocumentSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search relevant documents from the vector store and return summarized document snippets")
    public String searchDocuments(
            @ToolParam(description = "Question or keywords used for vector similarity search") String query,
            @ToolParam(description = "Top K documents to retrieve, default is 4") Integer topK) {

        if (!StringUtils.hasText(query)) {
            return "Document search failed: query must not be empty.";
        }
        if (vectorStore == null) {
            return "Document search failed: VectorStore is not injected.";
        }

        int resultTopK = normalizeTopK(topK);

        try {
            FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                    .query(query.trim())
                    .topK(resultTopK)
                    .filterExpression(filterExpressionBuilder.eq(METADATA_MODULE, MODULE_NAME).build())
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);
            List<Document> validDocuments = documents.stream()
                                .filter(Objects::nonNull)
                                .filter(document -> StringUtils.hasText(document.getText()))
                                .limit(resultTopK)
                                .toList();

            if (validDocuments.isEmpty()) {
                return """
                        Document search returned no results.
                        Query: %s
                        Scope: metadata %s=%s
                        """.formatted(query.trim(), VectorStoreManagerConstants.METADATA_MODULE, MODULE_NAME);
            }

            StringBuilder result = new StringBuilder();
            result.append("Document search result").append("\n");
            result.append("Query: ").append(query.trim()).append("\n");
            result.append("Matched documents: ").append(validDocuments.size()).append("\n\n");

            for (int i = 0; i < validDocuments.size(); i++) {
                Document document = validDocuments.get(i);
                String content = abbreviate(document.getText(), MAX_DOC_CHARS);
                result.append("Document ").append(i + 1).append(":").append("\n");
                result.append(content).append("\n");

                Object source = document.getMetadata().getOrDefault(VectorStoreManagerConstants.METADATA_SOURCE, "unknown");
                Object fileName = document.getMetadata().getOrDefault(VectorStoreManagerConstants.METADATA_FILE_NAME, "unknown");
                result.append("Source: ").append(source).append("\n");
                result.append("FileName: ").append(fileName).append("\n\n");
            }

            return result.toString().trim();
        }
        catch (RuntimeException exception) {
            return "Document search failed: " + exception.getMessage();
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, MAX_TOP_K));
    }

    private String abbreviate(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }
}
