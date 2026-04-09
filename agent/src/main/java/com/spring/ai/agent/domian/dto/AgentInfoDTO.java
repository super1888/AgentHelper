package com.spring.ai.agent.domian.dto;

import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/24
 */
@Builder
@Data
public class AgentInfoDTO {

    /**
     * agent id
     */
    private Long agentId;

    /**
     * agent name
     */
    private String agentName;

    /**
     * agent 描述
     */
    private String description;

    /**
     * agent 返回key
     */
    private String outputKey;

    /**
     * agent 指导介绍
     */
    private String instruction;

    /**
     * 控制是否在消息历史中包含中间推理
     */
    private Boolean returnReasoningContents ;

    /**
     * 包含上一个Agent的推理内容
     */
    private Boolean includeContents;

    /**
     * 大模型配置
     */
    private ChatModel model;

    /**
     * 工具集合类
     */
    private List<Object> methodTools;


    /**
     * 拦截器集合
     */
    private List<Interceptor> interceptors;

    /**
     * 是否记忆
     */
    private Boolean isMemory;

    /**
     * 记忆对象
     */
    private MemorySaver memorySaver;


    /**
     * 钩子
     */
    private List<Hook> hooks;


    /**
     * 特定格式返回输出
     */
    private Class<?> outputTypeClass;

    /**
     * 特定格式返回输出outputSchema 对象转json
     */
    private Class<?> outputSchemaClass;

    /**
     * 指定JSON模式返回
     */
    private String outputSchemaJson;

    /**
     * 特定格式返回输出inputSchema 对象转json
     */
    private Class<?> inputSchemaClass;

    /**
     * 指定JSON模式输入
     */
    private String inputSchemaJson;


    /**
     * 开启日志
     */
    private Boolean enableLogging;

    /**
     * 工具集合类
     */
    private List<ToolCallback> tools;


}
