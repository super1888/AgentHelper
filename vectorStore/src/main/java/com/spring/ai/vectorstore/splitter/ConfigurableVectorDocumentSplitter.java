package com.spring.ai.vectorstore.splitter;

import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.config.VectorStoreProperties.SplitMode;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 默认文档切分服务，支持固定长度、递归、语义和文档类型感知切分。
 */
@Service
public class ConfigurableVectorDocumentSplitter implements VectorDocumentSplitter {

    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern FAQ_PAIR_PATTERN = Pattern.compile("(?is)(?:^|\\n)\\s*(?:Q|问题|问)[:：]\\s*(.+?)(?:\\n)\\s*(?:A|答案|答)[:：]\\s*(.+?)(?=(?:\\n\\s*(?:Q|问题|问)[:：])|$)");
    private static final Pattern CODE_BOUNDARY_PATTERN = Pattern.compile("(?m)^(\\s*(?:public|private|protected|static|final|async|def|class|interface|enum|function|const|let|var|func|fn)\\b.*)$");

    private final VectorStoreProperties properties;
    private final TokenTextSplitter tokenTextSplitter;

    public ConfigurableVectorDocumentSplitter(VectorStoreProperties properties, TokenTextSplitter tokenTextSplitter) {
        this.properties = properties;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    /**
     * 根据文件类型优先使用领域切分策略，否则使用 YAML 中的通用切分模式。
     */
    @Override
    public List<Document> split(List<Document> documents, String extension) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        VectorStoreProperties.Split splitProperties = properties.getSplit();
        List<Document> typedChunks = splitByDocumentType(documents, normalizedExtension, splitProperties);
        if (!typedChunks.isEmpty()) {
            return typedChunks;
        }
        return switch (splitProperties.getMode()) {
            case FIXED -> splitFixed(documents);
            case RECURSIVE -> splitRecursive(documents);
            case SEMANTIC -> splitSemantic(documents);
            case AUTO -> tokenTextSplitter.apply(documents);
        };
    }

    private List<Document> splitByDocumentType(
            List<Document> documents,
            String extension,
            VectorStoreProperties.Split splitProperties
    ) {
        if (splitProperties.isFaqPairEnabled() && isFaq(extension)) {
            return splitFaq(documents);
        }
        if (splitProperties.isMarkdownHeadingEnabled() && isMarkdown(extension)) {
            return splitMarkdown(documents);
        }
        if (splitProperties.isCodeBlockEnabled() && isCode(extension)) {
            return splitCode(documents);
        }
        if (splitProperties.isPdfParagraphEnabled() && isPdf(extension)) {
            return splitPdfParagraphs(documents);
        }
        return List.of();
    }

    private List<Document> splitFixed(List<Document> documents) {
        int fixedLength = requirePositive(properties.getSplit().getFixedLength(), "fixedLength 必须大于 0");
        int overlap = Math.max(0, properties.getSplit().getOverlap());
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            String text = safeText(document);
            int start = 0;
            int index = 0;
            while (start < text.length()) {
                int end = Math.min(text.length(), start + fixedLength);
                chunks.add(newDocument(document, text.substring(start, end), Map.of("splitMode", "FIXED", "chunkIndex", index++)));
                if (end == text.length()) {
                    break;
                }
                start = Math.max(end - overlap, start + 1);
            }
        }
        return chunks;
    }

    private List<Document> splitRecursive(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            List<String> texts = recursiveSplit(safeText(document), 0);
            for (int index = 0; index < texts.size(); index++) {
                chunks.add(newDocument(document, texts.get(index), Map.of("splitMode", "RECURSIVE", "chunkIndex", index)));
            }
        }
        return chunks;
    }

    private List<String> recursiveSplit(String text, int separatorIndex) {
        int chunkSize = requirePositive(properties.getChunkSize(), "chunkSize 必须大于 0");
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        List<String> separators = properties.getSplit().getRecursiveSeparators();
        if (separators == null || separatorIndex >= separators.size()) {
            return fixedTextParts(text, chunkSize, Math.max(0, properties.getSplit().getOverlap()));
        }
        String separator = separators.get(separatorIndex);
        List<String> result = new ArrayList<>();
        for (String part : text.split(Pattern.quote(separator))) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String nextPart = properties.isKeepSeparator() ? part + separator : part;
            if (nextPart.length() > chunkSize) {
                result.addAll(recursiveSplit(nextPart, separatorIndex + 1));
            } else {
                result.add(nextPart.trim());
            }
        }
        return result;
    }

    private List<Document> splitSemantic(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            List<String> sentences = Arrays.stream(safeText(document).split("(?<=[。！？.!?])\\s*"))
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            if (sentences.isEmpty()) {
                continue;
            }
            List<Integer> breakpoints = semanticBreakpoints(sentences);
            int start = 0;
            int index = 0;
            for (Integer breakpoint : breakpoints) {
                chunks.add(newDocument(document, String.join("", sentences.subList(start, breakpoint + 1)), Map.of("splitMode", "SEMANTIC", "chunkIndex", index++)));
                start = breakpoint + 1;
            }
            if (start < sentences.size()) {
                chunks.add(newDocument(document, String.join("", sentences.subList(start, sentences.size())), Map.of("splitMode", "SEMANTIC", "chunkIndex", index)));
            }
        }
        return chunks;
    }

    private List<Integer> semanticBreakpoints(List<String> sentences) {
        int chunkSize = requirePositive(properties.getChunkSize(), "chunkSize 必须大于 0");
        double percentile = properties.getSplit().getSemanticBreakpointPercentile();
        int minLength = Math.max(1, (int) (chunkSize * Math.max(0.2D, Math.min(0.95D, percentile))));
        List<Integer> breakpoints = new ArrayList<>();
        int currentLength = 0;
        for (int index = 0; index < sentences.size(); index++) {
            currentLength += sentences.get(index).length();
            if (currentLength >= minLength && currentLength >= chunkSize) {
                breakpoints.add(index);
                currentLength = 0;
            }
        }
        return breakpoints;
    }

    private List<Document> splitMarkdown(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            Map<Integer, String> headingLevels = new LinkedHashMap<>();
            StringBuilder chunk = new StringBuilder();
            int index = 0;
            for (String line : safeText(document).split("\\R")) {
                Matcher matcher = MARKDOWN_HEADING_PATTERN.matcher(line);
                if (matcher.matches()) {
                    if (!chunk.isEmpty()) {
                        chunks.add(newDocument(document, chunk.toString().trim(), headingMetadata(headingLevels, index++)));
                        chunk.setLength(0);
                    }
                    int level = matcher.group(1).length();
                    headingLevels.keySet().removeIf(existingLevel -> existingLevel >= level);
                    headingLevels.put(level, matcher.group(2).trim());
                }
                chunk.append(line).append('\n');
            }
            if (!chunk.isEmpty()) {
                chunks.add(newDocument(document, chunk.toString().trim(), headingMetadata(headingLevels, index)));
            }
        }
        return chunks;
    }

    private Map<String, Object> headingMetadata(Map<Integer, String> headingLevels, int index) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("splitMode", "MARKDOWN_HEADING");
        metadata.put("chunkIndex", index);
        metadata.put("headingPath", String.join(" / ", headingLevels.values()));
        headingLevels.forEach((level, heading) -> metadata.put("heading" + level, heading));
        return metadata;
    }

    private List<Document> splitPdfParagraphs(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            List<String> paragraphs = Arrays.stream(safeText(document).split("\\R\\s*\\R"))
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            for (int index = 0; index < paragraphs.size(); index++) {
                chunks.add(newDocument(document, paragraphs.get(index), Map.of("splitMode", "PDF_PARAGRAPH", "chunkIndex", index)));
            }
        }
        return chunks;
    }

    private List<Document> splitCode(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            String text = safeText(document);
            List<Integer> starts = new ArrayList<>();
            Matcher matcher = CODE_BOUNDARY_PATTERN.matcher(text);
            while (matcher.find()) {
                starts.add(matcher.start());
            }
            if (starts.isEmpty()) {
                chunks.addAll(splitRecursive(List.of(document)));
                continue;
            }
            starts.add(text.length());
            for (int index = 0; index < starts.size() - 1; index++) {
                String codeBlock = text.substring(starts.get(index), starts.get(index + 1)).trim();
                if (StringUtils.hasText(codeBlock)) {
                    chunks.add(newDocument(document, codeBlock, Map.of("splitMode", "CODE_BLOCK", "chunkIndex", index)));
                }
            }
        }
        return chunks;
    }

    private List<Document> splitFaq(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            Matcher matcher = FAQ_PAIR_PATTERN.matcher(safeText(document));
            int index = 0;
            while (matcher.find()) {
                String text = "Q: " + matcher.group(1).trim() + "\nA: " + matcher.group(2).trim();
                chunks.add(newDocument(document, text, Map.of("splitMode", "FAQ_PAIR", "chunkIndex", index++)));
            }
            if (index == 0) {
                chunks.addAll(splitRecursive(List.of(document)));
            }
        }
        return chunks;
    }

    private List<String> fixedTextParts(String text, int fixedLength, int overlap) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + fixedLength);
            parts.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return parts.stream().filter(StringUtils::hasText).toList();
    }

    private Document newDocument(Document source, String text, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new HashMap<>(source.getMetadata());
        metadata.putAll(extraMetadata);
        return new Document(source.getId(), text, metadata);
    }

    private String safeText(Document document) {
        return document == null || document.getText() == null ? "" : document.getText();
    }

    private int requirePositive(int value, String message) {
        if (value <= 0) {
            throw VectorStoreException.badRequest(message);
        }
        return value;
    }

    private boolean isMarkdown(String extension) {
        return List.of("md", "markdown", "mdx").contains(extension);
    }

    private boolean isPdf(String extension) {
        return "pdf".equals(extension);
    }

    private boolean isFaq(String extension) {
        return List.of("faq", "qa", "qna").contains(extension);
    }

    private boolean isCode(String extension) {
        return List.of("java", "kt", "py", "js", "ts", "vue", "go", "rs", "cpp", "c", "h", "cs", "php", "rb", "scala").contains(extension);
    }
}
