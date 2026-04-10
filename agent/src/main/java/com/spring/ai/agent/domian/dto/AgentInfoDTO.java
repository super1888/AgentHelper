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
 * Agent 通用配置 DTO。
 *
 * <p>用于承载创建 ReactAgent 时所需的大部分参数，是当前项目中最核心的 Agent
 * 配置对象之一。</p>
 */
@Builder
@Data
public class AgentInfoDTO {

    /**
     * Agent 主键。
     */
    private Long agentId;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * Agent 描述。
     */
    private String description;

    /**
     * Agent 输出结果在状态中的保存 key。
     */
    private String outputKey;

    /**
     * Agent 系统指令。
     */
    private String instruction;

    /**
     * 是否在上下文中保留中间推理内容。
     */
    private Boolean returnReasoningContents;

    /**
     * 是否包含上游 Agent 的输出内容。
     */
    private Boolean includeContents;

    /**
     * 所使用的大模型实例。
     */
    private ChatModel model;

    /**
     * 基于方法扫描注册的工具对象集合。
     */
    private List<Object> methodTools;

    /**
     * 拦截器集合。
     */
    private List<Interceptor> interceptors;

    /**
     * 是否开启记忆能力。
     */
    private Boolean isMemory;

    /**
     * 记忆保存器。
     */
    private MemorySaver memorySaver;

    /**
     * Hook 集合。
     */
    private List<Hook> hooks;

    /**
     * 指定结构化输出类型。
     */
    private Class<?> outputTypeClass;

    /**
     * 指定输出 Schema 对应的 Java 类。
     */
    private Class<?> outputSchemaClass;

    /**
     * 指定输出 Schema 的 JSON 字符串。
     */
    private String outputSchemaJson;

    /**
     * 指定输入 Schema 对应的 Java 类。
     */
    private Class<?> inputSchemaClass;

    /**
     * 指定输入 Schema 的 JSON 字符串。
     */
    private String inputSchemaJson;

    /**
     * 是否开启 Agent 日志。
     */
    private Boolean enableLogging;

    /**
     * 直接注册的 ToolCallback 集合。
     */
    private List<ToolCallback> tools;
}
