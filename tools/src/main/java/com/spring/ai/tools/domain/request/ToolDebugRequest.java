package com.spring.ai.tools.domain.request;

import lombok.Data;

/**
 * 文件用途：工具在线调试请求对象
 */
@Data
public class ToolDebugRequest {

    private Long toolId;

    private String requestPayloadJson;

    private String sourceType;
}
