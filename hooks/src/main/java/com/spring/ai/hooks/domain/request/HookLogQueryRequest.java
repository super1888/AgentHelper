package com.spring.ai.hooks.domain.request;

import lombok.Data;

/**
 * 文件用途：Hook 日志查询请求
 */
@Data
public class HookLogQueryRequest {

    private Long hookId;
    private String sourceType;
    private Integer successFlag;
}
