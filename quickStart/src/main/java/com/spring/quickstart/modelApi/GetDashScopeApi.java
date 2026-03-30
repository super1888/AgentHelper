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
public class GetDashScopeApi {
  // 从配置文件中注入API密钥
    @Value("${spring.ai.dashscope.api-key}")  // 存储API密钥的私有变量
    private String apiKey;
    /**
     * 获取DashScopeApi实例的方法
     * @param getApiKey 传入的API密钥，如果为空则使用配置文件中的默认值
     * @return DashScopeApi 返回配置好的DashScopeApi实例
     */

        // 检查传入的API密钥是否为空，如果为空则使用配置文件中的默认值
    public  DashScopeApi getDashScopeApi(String getApiKey) {
        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = apiKey;
        }
        // 创建模型实例
        return DashScopeApi.builder()
                .apiKey(getApiKey)
                .build();
    }

}
