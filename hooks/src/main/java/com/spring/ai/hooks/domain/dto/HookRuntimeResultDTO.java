package com.spring.ai.hooks.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 运行时处理结果
 */
@Data
@Builder
public class HookRuntimeResultDTO {

    private String content;

    private Integer blocked;

    private String failureReason;

    private Integer matchedHookCount;
}
