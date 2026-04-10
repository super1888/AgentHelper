package com.spring.ai.tools.factory;

import com.spring.ai.tools.custom.DatabaseQueryTools;
import com.spring.ai.tools.custom.DocumentSearchTool;
import com.spring.ai.tools.custom.WebSearchTools;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Agent tool preset factory.
 *
 * <p>Provides a quick way to assemble web-search and database-query tools for
 * methodTools registration.</p>
 *
 * @author zhouqi
 * @since 2026/4/10
 */
public final class AgentToolPresetFactory {

    private AgentToolPresetFactory() {
    }

    public static List<Object> createSearchAndDatabaseTools() {
        return Arrays.asList(
                new WebSearchTools(),
                new DatabaseQueryTools()
        );
    }

    public static List<Object> createSearchDatabaseAndDocumentTools(VectorStore vectorStore) {
        return Arrays.asList(
                new WebSearchTools(),
                new DatabaseQueryTools(),
                new DocumentSearchTool(vectorStore)
        );
    }
}
