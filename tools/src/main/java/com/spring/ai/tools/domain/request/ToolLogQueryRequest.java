package com.spring.ai.tools.domain.request;

import lombok.Data;

/**
 * 文件用途：工具日志查询请求对象
 */
@Data
public class ToolLogQueryRequest {

    private Long toolId;

    private String sourceType;

    private Integer successFlag;
}
