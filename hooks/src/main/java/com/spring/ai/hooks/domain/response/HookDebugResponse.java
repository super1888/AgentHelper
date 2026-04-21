package com.spring.ai.hooks.domain.response;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 调试响应
 */
@Data
@Builder
public class HookDebugResponse {

    private Long hookId;
    private String hookCode;
    private String hookName;
    private Integer successFlag;
    private String executeStatus;
    private String responseText;
    private String failureReason;
    private Long elapsedMs;
    private String requestPayloadJson;
    private String responsePayloadJson;
    private Map<String, Object> tracePayload;
}
