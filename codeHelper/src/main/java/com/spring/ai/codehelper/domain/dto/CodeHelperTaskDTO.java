package com.spring.ai.codehelper.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 代码助手任务 DTO。
 */
@Data
@Builder
public class CodeHelperTaskDTO {

    private String taskId;

    private String title;

    private String status;

    private String detail;

    private List<String> changedFiles;
}
