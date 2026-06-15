package com.spring.ai.codehelper.domain.request;

import lombok.Data;

/**
 * 代码助手消息请求。
 */
@Data
public class CodeHelperMessageRequest {

    private String content;

    private String modelCode;

    private Boolean autoToolCall;
}
