package com.spring.ai.tools.agentTool;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallback;

/**
 * 创建agentTool 在工具调用中，一个Agent（"控制器"）将其他Agent视为工具（AgentTool），在需要时调用。控制器管理编排，而工具Agent执行特定任务并返回结果。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
public class CreatAgentTool {


    /**
     * 创建agentTool
     * @param agent
     * @return
     */
    public ToolCallback creatAgentTool(ReactAgent agent) {
        return AgentTool.getFunctionToolCallback(agent);
    }

}
