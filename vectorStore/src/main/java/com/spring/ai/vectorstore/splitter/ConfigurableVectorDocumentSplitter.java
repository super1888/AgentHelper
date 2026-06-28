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
 * 默认文档切分服务。
 *
 * <p>核心职责：</p>
 * <p>1. 接收文档解析器读取出来的原始 Document 列表。</p>
 * <p>2. 根据文件后缀优先选择“文件类型感知切分”，例如 Markdown 按标题、代码按函数或类、FAQ 按问答对。</p>
 * <p>3. 如果文件类型没有专属策略，再根据 YAML 中 app.vector-store.split.mode 选择固定长度、递归、语义或默认 Token 切分。</p>
 * <p>4. 每个切片都会保留原始元数据，并额外写入 splitMode、chunkIndex 等切分信息，方便后续检索和排查。</p>
 */
@Service
public class ConfigurableVectorDocumentSplitter implements VectorDocumentSplitter {

    // Markdown 标题识别规则。入参是一行文本，匹配 #、##、### 等标题，并提取标题层级和标题内容。
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    // FAQ 问答对识别规则。入参是整段文本，匹配 Q/A、问题/答案、问/答 等格式，确保一个问答对不会被拆开。
    private static final Pattern FAQ_PAIR_PATTERN = Pattern.compile("(?is)(?:^|\\n)\\s*(?:Q|问题|问)[:：]\\s*(.+?)(?:\\n)\\s*(?:A|答案|答)[:：]\\s*(.+?)(?=(?:\\n\\s*(?:Q|问题|问)[:：])|$)");
    // 代码结构识别规则。入参是源码文本，匹配类、接口、枚举、函数、方法等边界，尽量保留完整代码块。
    private static final Pattern CODE_BOUNDARY_PATTERN = Pattern.compile("(?m)^(\\s*(?:public|private|protected|static|final|async|def|class|interface|enum|function|const|let|var|func|fn)\\b.*)$");

    private final VectorStoreProperties properties;
    private final TokenTextSplitter tokenTextSplitter;

    public ConfigurableVectorDocumentSplitter(VectorStoreProperties properties, TokenTextSplitter tokenTextSplitter) {
        this.properties = properties;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    /**
     * 根据文件类型优先使用领域切分策略，否则使用 YAML 中的通用切分模式。
     *
     * @param documents 已完成解析和元数据补充的文档列表。每个 Document 的 text 是待切分文本，metadata 是文件名、模块名等上下文信息。
     * @param extension 文件后缀，例如 md、pdf、java、faq。该参数决定是否进入 Markdown、PDF、代码、FAQ 等专属切分策略。
     * @return 切分后的 Document 列表。返回的每个 Document 都会保留原始 metadata，并附加切分模式和序号。
     *
     * <p>处理步骤：</p>
     * <p>1. 空列表直接返回空结果，避免后续空指针。</p>
     * <p>2. 将后缀统一转小写，避免 PDF、Pdf、pdf 被当成不同类型。</p>
     * <p>3. 优先尝试文件类型感知切分，因为这类切分最能保留文档结构。</p>
     * <p>4. 如果没有命中文件类型策略，再走 YAML 中配置的通用切分模式。</p>
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
        // FAQ 优先级最高，因为问答对一旦被拆开，答案会失去对应问题，严重影响 RAG 准确性。
        if (splitProperties.isFaqPairEnabled() && isFaq(extension)) {
            return splitFaq(documents);
        }
        // Markdown 通过标题层级保留章节路径，让检索结果能知道内容属于哪个标题下。
        if (splitProperties.isMarkdownHeadingEnabled() && isMarkdown(extension)) {
            return splitMarkdown(documents);
        }
        // 代码文件按类、函数、方法边界切分，避免把一个完整函数拆成多个无意义片段。
        if (splitProperties.isCodeBlockEnabled() && isCode(extension)) {
            return splitCode(documents);
        }
        // PDF 解析后通常是自然段文本，优先按空行段落切分，减少跨段落混杂。
        if (splitProperties.isPdfParagraphEnabled() && isPdf(extension)) {
            return splitPdfParagraphs(documents);
        }
        return List.of();
    }

    private List<Document> splitFixed(List<Document> documents) {
        // fixedLength：每个 chunk 的最大字符长度；overlap：相邻 chunk 之间保留的重叠字符数，用于降低上下文断裂。
        int fixedLength = requirePositive(properties.getSplit().getFixedLength(), "fixedLength 必须大于 0");
        int overlap = Math.max(0, properties.getSplit().getOverlap());
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            // text：当前待切分文档的纯文本内容。
            String text = safeText(document);
            int start = 0;
            int index = 0;
            while (start < text.length()) {
                // end：当前切片结束位置，不能超过文本总长度。
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
        // 递归切分会按配置的分隔符逐级尝试，例如先按空行，再按换行，再按句号，最后才退化为固定长度。
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
        // text：当前递归层需要处理的文本；separatorIndex：当前使用第几个分隔符。
        int chunkSize = requirePositive(properties.getChunkSize(), "chunkSize 必须大于 0");
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        List<String> separators = properties.getSplit().getRecursiveSeparators();
        if (separators == null || separatorIndex >= separators.size()) {
            // 所有分隔符都无法继续细分时，使用固定长度兜底，保证不会产生过大的 chunk。
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
                // 当前片段仍然过长，进入下一级分隔符继续拆分。
                result.addAll(recursiveSplit(nextPart, separatorIndex + 1));
            } else {
                result.add(nextPart.trim());
            }
        }
        return result;
    }

    private List<Document> splitSemantic(List<Document> documents) {
        // 语义切分会先按句子切分，再按长度阈值聚合句子，尽量保证一个 chunk 表达一个完整语义片段。
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
        // sentences：已经拆好的句子列表；返回值是每个语义 chunk 的结束句子下标。
        int chunkSize = requirePositive(properties.getChunkSize(), "chunkSize 必须大于 0");
        double percentile = properties.getSplit().getSemanticBreakpointPercentile();
        // semanticBreakpointPercentile 用于控制语义块长度阈值，值越大，单个 chunk 越长。
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
        // Markdown 切分的核心是维护 headingLevels：记录当前所在的标题路径，例如 H1/H2/H3。
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            Map<Integer, String> headingLevels = new LinkedHashMap<>();
            StringBuilder chunk = new StringBuilder();
            int index = 0;
            for (String line : safeText(document).split("\\R")) {
                Matcher matcher = MARKDOWN_HEADING_PATTERN.matcher(line);
                if (matcher.matches()) {
                    if (!chunk.isEmpty()) {
                        // 遇到新标题时，先把上一个标题下积累的正文输出为一个 chunk。
                        chunks.add(newDocument(document, chunk.toString().trim(), headingMetadata(headingLevels, index++)));
                        chunk.setLength(0);
                    }
                    int level = matcher.group(1).length();
                    // 如果当前标题是二级标题，则清理二级及更深层级旧标题，保证标题路径准确。
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
        // headingLevels：当前 chunk 所属标题路径；index：当前文件内的 chunk 序号。
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("splitMode", "MARKDOWN_HEADING");
        metadata.put("chunkIndex", index);
        metadata.put("headingPath", String.join(" / ", headingLevels.values()));
        headingLevels.forEach((level, heading) -> metadata.put("heading" + level, heading));
        return metadata;
    }

    private List<Document> splitPdfParagraphs(List<Document> documents) {
        // PDF 已经由 reader 完成文本抽取，这里按空行识别自然段，避免将多个无关段落塞进一个 chunk。
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
        // 代码切分通过正则查找结构边界，切分结果尽量保留一个类、函数或方法的完整代码块。
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            String text = safeText(document);
            List<Integer> starts = new ArrayList<>();
            Matcher matcher = CODE_BOUNDARY_PATTERN.matcher(text);
            while (matcher.find()) {
                starts.add(matcher.start());
            }
            if (starts.isEmpty()) {
                // 没有识别到明确代码边界时，退回递归切分，避免整份代码过大导致嵌入失败。
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
        // FAQ 切分必须保持“问题 + 答案”成对出现，否则召回问题时可能拿不到对应答案。
        List<Document> chunks = new ArrayList<>();
        for (Document document : documents) {
            Matcher matcher = FAQ_PAIR_PATTERN.matcher(safeText(document));
            int index = 0;
            while (matcher.find()) {
                String text = "Q: " + matcher.group(1).trim() + "\nA: " + matcher.group(2).trim();
                chunks.add(newDocument(document, text, Map.of("splitMode", "FAQ_PAIR", "chunkIndex", index++)));
            }
            if (index == 0) {
                // 如果文件后缀是 FAQ 但内容不符合问答正则，则退回递归切分，保证仍有可入库内容。
                chunks.addAll(splitRecursive(List.of(document)));
            }
        }
        return chunks;
    }

    private List<String> fixedTextParts(String text, int fixedLength, int overlap) {
        // 固定长度兜底方法，主要供递归切分在所有分隔符都失效时使用。
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
        // source：原始文档；text：切片后的文本；extraMetadata：本次切分产生的附加元数据。
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
