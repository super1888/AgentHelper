package com.spring.ai.hooks.domain.request;

import lombok.Data;

/**
 * 文件用途：Hook 版本对比请求
 */
@Data
public class HookVersionCompareRequest {

    private Integer sourceVersionNo;
    private Integer targetVersionNo;
}
