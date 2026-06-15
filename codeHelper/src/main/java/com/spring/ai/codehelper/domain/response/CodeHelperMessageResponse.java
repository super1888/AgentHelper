package com.spring.ai.codehelper.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手消息响应。
 */
@Data
@Builder
public class CodeHelperMessageResponse {

    private String role;

    private String content;

    private String timestamp;
}
