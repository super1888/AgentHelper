package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 版本对比响应
 */
@Data
@Builder
public class HookVersionCompareResponse {

    private Integer sourceVersionNo;
    private Integer targetVersionNo;
    private String sourceSnapshotJson;
    private String targetSnapshotJson;
    private String diffSummary;
}
