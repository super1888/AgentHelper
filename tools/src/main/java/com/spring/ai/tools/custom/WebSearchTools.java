package com.spring.ai.tools.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Web search tool.
 *
 * <p>This is a first-pass implementation for agent use. It currently calls the
 * DuckDuckGo Instant Answer endpoint and can be replaced later with a formal
 * search provider.</p>
 *
 * @author zhouqi
 * @since 2026/4/10
 */
public class WebSearchTools {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     *
     * 在公共网络信息中搜索最新事实、摘要和主题参考资料
     * @param query
     * @param limit
     * @return
     */
    @Tool(description = "Search public web information for recent facts, summaries, and topic references")
    public String searchWeb(
            @ToolParam(description = "Question or keywords to search on the web") String query,
            @ToolParam(description = "Result size from 1 to 5, default is 3") Integer limit) {

        if (query == null || query.trim().isEmpty()) {
            return "Search failed: query must not be empty.";
        }

        int resultLimit = normalizeLimit(limit);
        String normalizedQuery = query.trim();

        try {
            String url = "https://api.duckduckgo.com/?q="
                    + URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8)
                    + "&format=json&no_html=1&skip_disambig=0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "springAi-agent-tools/1.0")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return buildFallbackResult(normalizedQuery,
                        "Search API call failed, HTTP status: " + response.statusCode());
            }

            return formatSearchResult(normalizedQuery, response.body(), resultLimit);
        }
        catch (Exception exception) {
            return buildFallbackResult(normalizedQuery,
                    "Search API is temporarily unavailable: " + exception.getMessage());
        }
    }

    private String formatSearchResult(String query, String body, int limit) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        StringBuilder result = new StringBuilder();
        result.append("Web search result").append("\n")
                .append("Query: ").append(query).append("\n");

        boolean hasContent = false;

        String abstractText = textValue(root, "AbstractText");
        String abstractSource = textValue(root, "AbstractSource");
        String abstractUrl = textValue(root, "AbstractURL");
        if (!abstractText.isEmpty()) {
            hasContent = true;
            result.append("1. Summary: ").append(abstractText).append("\n");
            if (!abstractSource.isEmpty()) {
                result.append("   Source: ").append(abstractSource).append("\n");
            }
            if (!abstractUrl.isEmpty()) {
                result.append("   Link: ").append(abstractUrl).append("\n");
            }
        }

        JsonNode relatedTopics = root.path("RelatedTopics");
        int index = hasContent ? 2 : 1;
        if (relatedTopics.isArray()) {
            for (JsonNode topic : relatedTopics) {
                if (index > limit) {
                    break;
                }

                JsonNode candidate = topic.hasNonNull("Text") ? topic : firstChildWithText(topic.path("Topics"));
                if (candidate == null) {
                    continue;
                }

                String text = textValue(candidate, "Text");
                if (text.isEmpty()) {
                    continue;
                }

                hasContent = true;
                result.append(index).append(". ").append(text).append("\n");
                String firstUrl = textValue(candidate, "FirstURL");
                if (!firstUrl.isEmpty()) {
                    result.append("   Link: ").append(firstUrl).append("\n");
                }
                index++;
            }
        }

        if (!hasContent) {
            return buildFallbackResult(query,
                    "No structured search result was returned. Replace with a formal search provider later.");
        }

        result.append("Note: this is a basic web search tool and the result is for preliminary agent reference.");
        return result.toString();
    }

    private JsonNode firstChildWithText(JsonNode topics) {
        if (!topics.isArray()) {
            return null;
        }
        for (JsonNode child : topics) {
            if (child.hasNonNull("Text")) {
                return child;
            }
        }
        return null;
    }

    private String buildFallbackResult(String query, String reason) {
        return """
                Web search tool returned a fallback result
                Query: %s
                Status: %s
                Suggestions:
                1. Replace this with a formal search API
                2. Add result reranking and source credibility filtering
                3. Add cache support to avoid repeated searches
                """.formatted(query, reason);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 3;
        }
        return Math.max(1, Math.min(limit, 5));
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "" : field.asText("");
    }
}
