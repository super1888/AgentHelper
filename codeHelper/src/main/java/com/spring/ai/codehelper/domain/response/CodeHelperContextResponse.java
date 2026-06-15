package com.spring.ai.codehelper.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手上下文响应。
 */
@Data
@Builder
public class CodeHelperContextResponse {

    private String sessionId;

    private String summary;

    private List<CodeHelperMessageResponse> recentMessages;

    private List<CodeHelperTaskResponse> tasks;
}
