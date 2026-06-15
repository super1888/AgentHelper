package com.spring.ai.codehelper.domain.request;

import lombok.Data;

/**
 * 代码助手上下文压缩请求。
 */
@Data
public class CodeHelperCompactRequest {

    private String summaryHint;
}
