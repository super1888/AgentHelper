package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 已保存模型配置的调用测试请求。
 */
@Data
public class ModelTestRequest {

    /**
     * 测试提示词；为空时走系统默认健康检查提示词。
     */
    private String testPrompt;
}
