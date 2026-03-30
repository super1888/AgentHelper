package com.spring.quickstart.agent;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.spring.quickstart.interceptor.ToolErrorInterceptor;
import com.spring.quickstart.tools.CalculatorTools;

import com.spring.quickstart.chatModel.GetDashScopeChatModel;
import com.spring.quickstart.model.dto.AgentInfoDTO;
import jakarta.annotation.Resource;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */
@Component
public class Agent {

    @Resource
    private GetDashScopeChatModel getDashScopeChatModel;

    /**
     * 简单的 agent
     *
     * @return
     */
    public ReactAgent creatAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("Agent")
                .model(getDashScopeChatModel.getModel())
                .systemPrompt("你是一个有帮助的助手")
                .build();

        return agent;

    }

    /**
     * 简单的 工具agent
     *
     * @return
     */
    public ReactAgent creatAgentTool() {

        CalculatorTools calculatorTools = new CalculatorTools();
        ToolErrorInterceptor toolErrorInterceptor = new ToolErrorInterceptor();
        ReactAgent agent = ReactAgent.builder()
                .name("search_agent")
                .model(getDashScopeChatModel.getSeniorModel())
                .methodTools(calculatorTools)
                .interceptors(toolErrorInterceptor)
                .build();

        return agent;

    }

    /**
     * 简单的 工具agent
     *
     * @return
     */
    public ReactAgent creatAgentPrompt() {

        String instruction = """
                你是一个经验丰富的JavaAI软件开发工程师 面试经验丰富。

                在回答问题时，请：
                1. 首先理解用户的核心需求
                2. 结合目前发展趋势常见问题
                3. 提供清晰的建议和理由
                4. 如果需要更多信息，主动询问

                保持专业、友好的语气。
                """;

        CalculatorTools calculatorTools = new CalculatorTools();
        ToolErrorInterceptor toolErrorInterceptor = new ToolErrorInterceptor();
        ReactAgent agent = ReactAgent.builder()
                .name("search_agent")
                .model(getDashScopeChatModel.getSeniorModel())
                .methodTools(calculatorTools)
                .interceptors(toolErrorInterceptor)
                .instruction(instruction)
                .build();

        return agent;

    }

    /**
     * 自定义agent
     *
     * @return
     */
    public ReactAgent customAgent(AgentInfoDTO agentInfoDTO) throws Exception {

        Builder builder = ReactAgent.builder();

        if (agentInfoDTO.getAgentName() == null) {
            throw new Exception("agentName不能为空");
        }

        builder.name(agentInfoDTO.getAgentName());

        builder.model(agentInfoDTO.getModel());

        builder.methodTools(agentInfoDTO.getTools().toArray());

        builder.interceptors(agentInfoDTO.getInterceptors());

        builder.instruction(agentInfoDTO.getInstruction());

        if (agentInfoDTO.getOutputTypeClass() != null) {
            builder.outputType(agentInfoDTO.getOutputTypeClass());
        }

        if (agentInfoDTO.getOutputSchemaClass() != null) {
            BeanOutputConverter<?> outputConverter = new BeanOutputConverter<>(agentInfoDTO.getOutputSchemaClass());
            String format = outputConverter.getFormat();
            builder.outputSchema(format);
        }

        if (agentInfoDTO.getIsMemory() != null && agentInfoDTO.getIsMemory()) {
            builder.saver(new MemorySaver());
        }

        if (agentInfoDTO.getHooks() != null) {
            builder.hooks(agentInfoDTO.getHooks());
        }

        return builder.build();

    }


}
