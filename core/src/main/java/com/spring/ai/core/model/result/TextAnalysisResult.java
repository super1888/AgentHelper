package com.spring.ai.core.model.result;

import java.util.List;
import lombok.Data;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/26
 */
@Data
public class TextAnalysisResult {

    private String summary;
    private List<String> keywords;
    private String sentiment;
    private Double confidence;

}
