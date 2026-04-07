package com.spring.quickstart.model.dto;

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
     * 添加角色描述
     */
    private String instruction;

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
     * 开启日志
     */
    private Boolean enableLogging;

    /**
     * 工具集合类
     */
    private List<ToolCallback> tools;


}
