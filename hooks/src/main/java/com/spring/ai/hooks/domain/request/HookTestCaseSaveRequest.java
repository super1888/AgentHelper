package com.spring.ai.hooks.domain.request;

import java.util.Map;
import lombok.Data;

/**
 * 文件用途：Hook 测试用例保存请求
 */
@Data
public class HookTestCaseSaveRequest {

    private String caseName;
    private Map<String, Object> inputPayload;
    private Map<String, Object> contextPayload;
    private Integer expectedSuccess;
    private String expectedResponseContains;
    private Integer enabled;
}
