package com.spring.quickstart.modelApi;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
@Component
public class GetDeepSeekApi {

    @Value("${spring.ai.deepseek.api-key}")
    private String apiKey;

    private DashScopeApi getDashScopeApi(String getApiKey) {

        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = apiKey;
        }
        // 创建模型实例
        return DashScopeApi.builder()
                .apiKey(getApiKey)
                .build();
    }

}
