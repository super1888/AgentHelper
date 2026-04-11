package com.spring.quickstart.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import com.spring.ai.core.facotry.GetDashScopeChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class A2ANacosConfig {

    public static final String A2A_AGENT_NAME = "a2a_data_analysis_agent";

    @Resource
    private GetDashScopeChatModel getDashScopeChatModel;

    @Bean(name = A2A_AGENT_NAME)
    public ReactAgent a2aDataAnalysisAgent() {
        return ReactAgent.builder()
                .name(A2A_AGENT_NAME)
                .model(getDashScopeChatModel.getSeniorModel())
                .description("A2A data analysis agent registered in Nacos")
                .instruction("你是一个专业的数据分析与知识问答智能体。"
                        + "请基于用户问题给出清晰、准确、结构化的回答。"
                        + "涉及统计、归纳、总结时优先输出结论和关键依据。")
                .outputKey("messages")
                .build();
    }
}
