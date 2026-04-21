package com.spring.ai.hooks.domain.request;

import java.util.Map;
import lombok.Data;

/**
 * 文件用途：Hook 调试请求
 */
@Data
public class HookDebugRequest {

    private Long hookId;
    private String requestPayloadJson;
    private Map<String, Object> contextPayload;
    private String sourceType;
    private String agentCode;
    private String sessionCode;
}
