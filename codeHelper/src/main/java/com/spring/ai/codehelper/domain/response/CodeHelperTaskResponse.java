package com.spring.ai.codehelper.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手任务响应。
 */
@Data
@Builder
public class CodeHelperTaskResponse {

    private String taskId;

    private String title;

    private String status;

    private String detail;

    private List<String> changedFiles;
}
