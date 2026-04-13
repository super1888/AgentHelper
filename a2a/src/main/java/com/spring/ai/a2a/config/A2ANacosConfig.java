package com.spring.ai.a2a.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import com.spring.ai.common.constants.A2ANameConstants;
import com.spring.ai.core.facotry.GetDashScopeChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 此方法注册了一个agent 作为a2a的提供方
 */
@Configuration
public class A2ANacosConfig {

    @Resource
    private GetDashScopeChatModel getDashScopeChatModel;

    @Bean(name = A2ANameConstants.DATA_ANALYSIS_AGENT)
    public ReactAgent a2aDataAnalysisAgent() {
        return ReactAgent.builder()
                .name(A2ANameConstants.DATA_ANALYSIS_AGENT)
                .model(getDashScopeChatModel.getSeniorModel())
                .description("A2A data analysis agent registered in Nacos")
                .instruction("你是一个专业的数据分析与知识问答智能体。"
                        + "请基于用户问题给出清晰、准确、结构化的回答。"
                        + "涉及统计、归纳、总结时优先输出结论和关键依据。")
                .outputKey("messages")
                .build();
    }
}
