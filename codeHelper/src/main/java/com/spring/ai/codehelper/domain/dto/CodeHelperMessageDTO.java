package com.spring.ai.codehelper.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手消息 DTO。
 */
@Data
@Builder
public class CodeHelperMessageDTO {

    private String role;

    private String content;

    private String timestamp;
}
