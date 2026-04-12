package com.spring.quickstart;


import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.spring.ai.agent.domian.dto.AgentInfoDTO;
import com.spring.ai.agent.factory.AgentFactory;
import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.core.facotry.GetChatModel;
import com.spring.ai.core.facotry.GetDashScopeChatModel;
import com.spring.ai.core.domain.dto.AssistantMessageDTO;
import com.spring.ai.core.domain.result.PoemOutputResult;
import com.spring.ai.core.domain.result.TextAnalysisResult;
import com.spring.ai.hooks.custom.agentHook.LoggingHook;
import com.spring.ai.hooks.custom.messagesModelHook.MessageTrimmingHook;
import com.spring.ai.hooks.custom.messagesModelHook.RAGMessagesHook;
import com.spring.ai.hooks.custom.messagesModelHook.TextFilterHook;

import com.spring.ai.interceptors.custom.modelInterceptor.RAGModelInterceptor;
import com.spring.ai.interceptors.custom.toolInterceptor.ToolErrorInterceptor;
import com.spring.ai.tools.custom.CalculatorTools;
import com.spring.ai.tools.custom.LocalTools;
import com.spring.ai.tools.custom.PythonTool;
import com.spring.ai.tools.custom.SendEmailTools;
import com.spring.ai.tools.custom.WeatherTools;
import com.spring.quickstart.userMessage.MessageType;
import jakarta.annotation.Resource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;


@SpringBootTest(classes = QuickStartApplication.class)
class QuickStartApplicationTests {

    private AgentInfoDTO getAgent() {
        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("学习agent").model(getDashScopeChatModel.getSeniorModel())
                .methodTools(Arrays.asList(new CalculatorTools(), new LocalTools(), new WeatherTools()))
                .interceptors(Arrays.asList(new ToolErrorInterceptor(), new ToolErrorInterceptor()))
                .instruction("你是一个学习助手，请根据用户的问题，使用工具回答用户的问题").build();
        return agentInfoDTO;
    }


    @Resource
    private AgentFactory agent;

    @Resource
    private GetDashScopeChatModel getDashScopeChatModel;

    @Resource
    private GetChatModel getChatModel;

    @Resource
    private VectorStore vectorStore;


    @Test
    void Agent1() {
        ReactAgent reactAgent = ReactAgent.builder()
                .name("Agent")
                .model(getDashScopeChatModel.getModel())
                .systemPrompt("你是一个有帮助的助手")
                .build();

        try {
            // 使用字符串
            AssistantMessage response1 = reactAgent.call("你好");
            System.out.println(response1.getText());
            System.out.println("------------------");

            // 使用 UserMessage
            UserMessage userMsg = new UserMessage("帮我写一首诗");
            AssistantMessage response2 = reactAgent.call(userMsg);
            System.out.println(response2.getText());
            System.out.println("------------------");
            // 使用消息列表
            List<Message> messages = List.of(
                    new UserMessage("我喜欢春天"),
                    new UserMessage("写一首关于春天的诗")
            );
            Prompt prompt = new Prompt();
            AssistantMessage response3 = reactAgent.call(messages);
            System.out.println(response3.getText());

        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent2() {

        ReactAgent reactAgent = ReactAgent.builder()
                .name("search_agent")
                .model(getDashScopeChatModel.getSeniorModel())
                .build();
        try {
            // 使用字符串
            AssistantMessage response1 = reactAgent.call("合肥的天气如何");
            System.out.println(response1.getText());
            System.out.println("------------------");

            // 使用字符串
            AssistantMessage response2 = reactAgent.call("给我一个随机数");
            System.out.println(response2.getText());
            System.out.println("------------------");


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent3() {
        String instruction = """
                你是一个经验丰富的JavaAI软件开发工程师 面试经验丰富。

                在回答问题时，请：
                1. 首先理解用户的核心需求
                2. 结合目前发展趋势常见问题
                3. 提供清晰的建议和理由
                4. 如果需要更多信息，主动询问

                保持专业、友好的语气。
                """;

        ReactAgent reactAgent = ReactAgent.builder()
                .name("search_agent")
                .model(getDashScopeChatModel.getSeniorModel())
                .instruction(instruction)
                .build();
        try {
            // 使用字符串
            AssistantMessage response1 = reactAgent.call("你觉得javaAI工程师面试会问什么");
            System.out.println(response1.getText());
            System.out.println("------------------");

        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }


    @Test
    void Agent4() {

        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("学习agent").model(getDashScopeChatModel.getSeniorModel())
                .methodTools(Arrays.asList(new CalculatorTools(), new LocalTools(), new WeatherTools()))
                .interceptors(Arrays.asList(new ToolErrorInterceptor(), new ToolErrorInterceptor()))
                .instruction("你是一个学习助手，请根据用户的问题，使用工具回答用户的问题").build();
        ReactAgent reactAgent = null;
        try {
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            // 使用字符串
            AssistantMessage response1 = reactAgent.call("你觉得javaAI工程师面试会问什么");
            System.out.println(response1.getText());
            System.out.println("------------------");
            // UserMessage 输入
            UserMessage userMessage = new UserMessage("你觉得javaAI工程师发展前景怎么样");
            AssistantMessage response2 = reactAgent.call(userMessage);
            System.out.println(response2.getText());
            System.out.println("------------------");
            // 多个消息
            List<Message> messages = List.of(
                    new UserMessage("我想了解 Java 多线程"),
                    new UserMessage("特别是线程池的使用")
            );
            AssistantMessage response3 = reactAgent.call(messages);
            System.out.println(response3.getText());
            System.out.println("------------------");


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }


    @Test
    void Agent5() {

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Optional<OverAllState> result = reactAgent.invoke("帮我写一首诗");

            if (result.isPresent()) {
                OverAllState state = result.get();

                // 访问消息历史
                Optional<Object> messages = state.value("messages");
                List<Message> messageList = (List<Message>) messages.get();
                messageList.forEach(System.out::println);

                // 访问自定义状态
                Optional<Object> customData = state.value("custom_key");

                System.out.println("完整状态：" + state);
            }


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent6() {

        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            AssistantMessage weather = reactAgent.call("给我地址", runnableConfig);
            System.out.println(weather.getText());


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent7() {

        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            agentInfoDTO.setOutputTypeClass(PoemOutputResult.class);
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            AssistantMessage weather = reactAgent.call("你的角色是什么", runnableConfig);
            System.out.println(weather.getText());


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent8() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            agentInfoDTO.setOutputSchemaClass(TextAnalysisResult.class);
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            AssistantMessage weather = reactAgent.call("帮我理一下aiAgent工程师学习路线", runnableConfig);
            System.out.println(weather.getText());


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void Agent9() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            agentInfoDTO.setIsMemory(true);
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            AssistantMessage response1 = reactAgent.call("你是张三", runnableConfig);
            System.out.println(response1.getText());
            AssistantMessage response2 = reactAgent.call("那你是谁", runnableConfig);
            System.out.println(response2.getText());


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void Agent10() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            agentInfoDTO.setHooks(Arrays.asList(new LoggingHook(), new MessageTrimmingHook()));
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            AssistantMessage response1 = reactAgent.call("你是张三", runnableConfig);
            System.out.println(response1.getText());
            AssistantMessage response2 = reactAgent.call("那你是谁", runnableConfig);
            System.out.println(response2.getText());


        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 流式输出示例
     * <p>
     * AGENT_MODEL_STREAMING	模型推理的流式增量内容 AGENT_MODEL_FINISHED	模型推理完成，可获取全量内容 AGENT_TOOL_STREAMING	工具调用的流式增量内容 AGENT_TOOL_FINISHED	工具调用完成
     * AGENT_HOOK_STREAMING	Hook 节点的流式增量内容 AGENT_HOOK_FINISHED	Hook 节点完成
     */
    @Test
    void Agent11() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        // 1. 创建计数器，等待异步流执行完成
        CountDownLatch latch = new CountDownLatch(1);
        try {
            ReactAgent reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
            Flux<NodeOutput> stream = reactAgent.stream("ai开发的复杂任务");
            stream.subscribe(
                    output -> {
                        // 流式输出处理
                        if (output instanceof StreamingOutput streamingOutput) {
                            OutputType type = streamingOutput.getOutputType();
                            // 处理模型推理的流式输出
                            if (type == OutputType.AGENT_MODEL_STREAMING) {
                                // 流式增量内容，逐步显示
                                System.out.print(streamingOutput.message().getText());
                            } else if (type == OutputType.AGENT_MODEL_FINISHED) {
                                // 模型推理完成，可获取完整响应
                                System.out.println("\n模型输出完成");
                            }

                            // 处理工具调用完成（目前不支持 STREAMING）
                            if (type == OutputType.AGENT_TOOL_FINISHED) {
                                System.out.println("工具调用完成: " + output.node());
                            }

                            // 对于 Hook 节点，通常只关注完成事件（如果Hook没有有效输出可以忽略）
                            if (type == OutputType.AGENT_HOOK_FINISHED) {
                                System.out.println("Hook 执行完成: " + output.node());
                            }
                        }
                    },
                    error -> {
                        // ============== 修复点2：强制打印错误 ==============
                        System.err.println("\n【执行错误】: " + error.getMessage());
                        error.printStackTrace();
                        latch.countDown();
                    },
                    () -> {
                        System.out.println("\n✅ Agent 执行全部完成");
                        latch.countDown(); // 执行完成释放
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // ============== 修复点3：阻塞主线程，等待异步执行完毕 ==============
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void Agent12() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        // 1. 创建计数器，等待异步流执行完成
        CountDownLatch latch = new CountDownLatch(1);
        try {
            ReactAgent reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
            Flux<NodeOutput> stream = reactAgent.stream("我地址的天气如何");
            stream.subscribe(
                    output -> {
                        // 流式输出处理
                        if (output instanceof StreamingOutput streamingOutput) {
                            OutputType type = streamingOutput.getOutputType();
                            Message message = streamingOutput.message();
                            // 处理模型推理的流式输出
                            if (type == OutputType.AGENT_MODEL_STREAMING) {
                                if (message instanceof AssistantMessage assistantMessage) {
                                    // 检查是否为 Thinking 消息
                                    Object reasoningContent = assistantMessage.getMetadata().get("reasoningContent");
                                    if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                                        System.out.print("[Thinking] " + reasoningContent);
                                    } else {
                                        // 普通模型响应（增量内容）
                                        System.out.print(assistantMessage.getText());
                                    }
                                }
                            } else if (type == OutputType.AGENT_MODEL_FINISHED) {
                                if (message instanceof AssistantMessage assistantMessage) {
                                    if (assistantMessage.hasToolCalls()) {
                                        // 工具调用请求
                                        assistantMessage.getToolCalls().forEach(toolCall -> {
                                            System.out.println("[Tool Call] " + toolCall.name() + ": " + toolCall.arguments());
                                        });
                                    } else {
                                        // 模型完整响应
                                        System.out.println("\n[Model Finished]");
                                    }
                                }
                            }

                            // 处理工具调用完成（目前不支持 STREAMING）
                            if (type == OutputType.AGENT_TOOL_FINISHED) {
                                if (message instanceof ToolResponseMessage toolResponse) {
                                    toolResponse.getResponses().forEach(response -> {
                                        System.out.println("[Tool Result] " + response.name() + ": " + response.responseData());
                                    });
                                }
                            }

                            // 对于 Hook 节点，通常只关注完成事件（如果Hook没有有效输出可以忽略）
                            if (type == OutputType.AGENT_HOOK_FINISHED) {
                                System.out.println("Hook 执行完成: " + output.node());
                            }
                        }
                    },
                    error -> {
                        // ============== 修复点2：强制打印错误 ==============
                        System.err.println("\n【执行错误】: " + error.getMessage());
                        error.printStackTrace();
                        latch.countDown();
                    },
                    () -> {
                        System.out.println("\n✅ Agent 执行全部完成");
                        latch.countDown(); // 执行完成释放
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // ============== 修复点3：阻塞主线程，等待异步执行完毕 ==============
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void Agent13() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 使用字符串直接调用
//        String response = chatModel.call("介绍一下Spring框架");
//        System.out.println(response);

        // 创建 Prompt
        Prompt prompt = new Prompt(new UserMessage("解释什么是微服务架构"));

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);

    }

    @Test
    void Agent14() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 使用字符串直接调用
//        String response = chatModel.call("介绍一下Spring框架");
//        System.out.println(response);
        List<Message> messages = MessageType.getAbstractMessage(
                new SystemMessage("你是一个诗人，你只会诗相关的技能，别人问其他问题都表示不会回答"),
                new UserMessage("springIOC是什么意思"), null, null);

        // 创建 Prompt
        Prompt prompt = new Prompt(messages);

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);

    }

    @Test
    void Agent15() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 使用字符串直接调用
        UserMessage build = UserMessage.builder().text("写一首关于ai的诗").metadata(Map.of("user_id", "123", "session_id", "123")).build();
        List<Message> messages = MessageType.getAbstractMessage(
                new SystemMessage("你是一个诗人，你只会诗相关的技能，别人问其他问题都表示不会回答"),
                build, null, null);

        // 创建 Prompt
        Prompt prompt = new Prompt(messages);

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);

    }

    @Test
    void Agent16() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 使用字符串直接调用
        UserMessage userMessage = UserMessage.builder().text("当地天气如何").metadata(Map.of("user_id", "123", "session_id", "123")).build();
        ToolCall toolCall1 = new ToolCall("tool_001", "function", "getAddress", "");
        ToolCall toolCall2 = new ToolCall("tool_001", "function", "weatherForLocationTool", "");

        AssistantMessage assistantMessage = AssistantMessageDTO.getAssistantMessage("", null, Arrays.asList(toolCall1, toolCall2),
                null);
        List<Message> messages = MessageType.getAbstractMessage(
                new SystemMessage("你是ai助手"),
                userMessage, assistantMessage, null);

        // 创建 Prompt
        Prompt prompt = new Prompt(messages);

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);

    }

    @Test
    void Agent17() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 使用字符串直接调用
        UserMessage userMessage = UserMessage.builder().text("当地天气如何").metadata(Map.of("user_id", "123", "session_id", "123")).build();
        ToolCall toolCall1 = new ToolCall("tool_001", "function", "getAddress", "");
        ToolCall toolCall2 = new ToolCall("tool_001", "function", "weatherForLocationTool", "");

        AssistantMessage assistantMessage = AssistantMessageDTO.getAssistantMessage("", null, Arrays.asList(toolCall1, toolCall2),
                null);
        List<Message> messages = MessageType.getAbstractMessage(
                new SystemMessage("你是ai助手"),
                userMessage, assistantMessage, null);

        // 创建 Prompt
        Prompt prompt = new Prompt(messages);

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);

        // 获取token信息
        ChatResponseMetadata metadata = response.getMetadata();
        if (metadata != null && metadata.getUsage() != null) {
            System.out.println("Input tokens: " + metadata.getUsage().getPromptTokens());
            System.out.println("Output tokens: " + metadata.getUsage().getCompletionTokens());
            System.out.println("Total tokens: " + metadata.getUsage().getTotalTokens());
        }

    }


    @Test
    void Agent18() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();

        Flux<ChatResponse> result = chatModel.stream(new Prompt("写一个推理的的小故事 不要告诉我答案 "));

        CountDownLatch latch = new CountDownLatch(1);

        result.subscribe(
                chunk -> {
                    String content = chunk.getResult().getOutput().getText();
                    System.out.print(content);
                },
                error -> {
                    // ============== 修复点2：强制打印错误 ==============
                    System.err.println("\n【执行错误】: " + error.getMessage());
                    error.printStackTrace();
                    latch.countDown();
                },
                () -> {
                    System.out.println("\n✅ Agent 执行全部完成");
                    latch.countDown(); // 执行完成释放
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * ToolResponseMessage 属性：
     * <p>
     * responses: ToolResponse 对象列表，每个包含： id: 工具调用 ID（必须与 AIMessage 中的工具调用 ID 匹配） name: 调用的工具名称 responseData: 工具调用的字符串化输出
     */
    @Test
    void Agent19() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();

        // 在模型进行工具调用后
        AssistantMessage aiMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall(
                                "call_123",
                                "tool",
                                "get_weather",
                                "\"location\": \"新加坡\"}"
                        )
                ))
                .build();

        // 执行工具并创建结果消息
        String weatherResult = "晴朗，22°C";
        ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                .responses(List.of(
                        new ToolResponse("call_123", "get_weather", weatherResult)
                ))
                .build();

        // 继续对话
        List<Message> messages = List.of(
                new UserMessage("旧金山的天气怎么样？"),
                aiMessage,      // 模型的工具调用
                toolMessage     // 工具执行结果
        );
        ChatResponse call = chatModel.call(new Prompt(messages));
        AssistantMessage assistantMessage = call.getResult().getOutput();
        String text = assistantMessage.getText();
        System.out.println("输出：" + text);
        if (assistantMessage.hasToolCalls()) {
            for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                System.out.println("Tool: " + toolCall.name());
                System.out.println("Args: " + toolCall.arguments());
                System.out.println("ID: " + toolCall.id());
            }
        }

    }

    @Test
    void Agent20() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();

        try {
            // 从 URL
            UserMessage message3 = UserMessage.builder()
                    .text("描述这张图片的内容。")
                    .media(Media.builder()
                            .mimeType(MimeTypeUtils.IMAGE_JPEG)
                            .data(new URL("https://example.com/image.jpg"))
                            .build())
                    .build();

            // 从本地文件
            UserMessage message2 = UserMessage.builder()
                    .text("描述这张图片的内容。")
                    .media(new Media(
                            MimeTypeUtils.IMAGE_JPEG,
                            new ClassPathResource("images/photo.jpg")
                    ))
                    .build();

            UserMessage message1 = UserMessage.builder()
                    .text("描述这段音频的内容。")
                    .media(new Media(
                            MimeTypeUtils.parseMimeType("audio/wav"),
                            new ClassPathResource("audio/recording.wav")
                    ))
                    .build();
            UserMessage message = UserMessage.builder()
                    .text("描述这段视频的内容。")
                    .media(Media.builder()
                            .mimeType(MimeTypeUtils.parseMimeType("video/mp4"))
                            .data(new URL("https://example.com/path/to/video.mp4"))
                            .build())
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent21() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();

        List<Message> conversationHistory = new ArrayList<>();

        // 第一轮对话
        conversationHistory.add(new UserMessage("你好！"));
        ChatResponse response1 = chatModel.call(new Prompt(conversationHistory));
        conversationHistory.add(response1.getResult().getOutput());

        // 第二轮对话
        conversationHistory.add(new UserMessage("你能帮我学习 Java 吗？"));
        ChatResponse response2 = chatModel.call(new Prompt(conversationHistory));
        conversationHistory.add(response2.getResult().getOutput());

        // 第三轮对话
        conversationHistory.add(new UserMessage("从哪里开始？"));
        ChatResponse response3 = chatModel.call(new Prompt(conversationHistory));
        System.out.println(response3.getResult().getOutput().getText());

    }

    @Test
    void Agent22() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
//        ChatClient chatClient = ChatClient.create(chatModel);
//
//        String content = chatClient.prompt("What day is tomorrow?").tools(new DateTimeTools()).call().content();
//        System.out.println(content);
        List<String> list = Arrays.asList("aa", "bb");
        String[] array = list.toArray(new String[0]);
        String response = ChatClient.create(chatModel)
                .prompt("给张三发封邮件 标题内容你定一下 让他还钱")
                .tools(new SendEmailTools())
//                .toolNames(array)
                .call()
                .content();

        System.out.println(response);

    }

    @Test
    void Agent23() {
        String threadId = "thread_123";
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata("user_id", "2")
                .build();

        AgentInfoDTO agentInfoDTO = getAgent();
        ReactAgent reactAgent = null;
        try {
            agentInfoDTO.setHooks(Arrays.asList(new TextFilterHook()));
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
            AssistantMessage result = reactAgent.call("我要投诉");
            System.out.println(result.getText());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void Agent24() {
        String threadId = "thread_123";
        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("skills-integration-agent")
                .model(getDashScopeChatModel.getSeniorModel())
                .enableLogging(true).build();
        ReactAgent reactAgent = null;
        try {
            // 用户加以引导
            SystemPromptTemplate customTemplate = SystemPromptTemplate.builder()
                    .template("## 可用技能\n{skills_list}\n\n## 加载说明\n{skills_load_instructions}")
                    .build();
            // 指定目录配置
            SkillRegistry registry11 = FileSystemSkillRegistry.builder()
                    .userSkillsDirectory("/home/user/saa/skills")
                    .projectSkillsDirectory("/app/project/skills")
                    .systemPromptTemplate(customTemplate)
                    .build();
            // 1. 技能注册表：从 classpath:skills 加载（如 src/main/resources/skills/）
            SkillRegistry registry = ClasspathSkillRegistry.builder()
                    .classpathPath("skills")
                    .build();

            // 2. Skills Hook：注册 read_skill 工具并注入技能列表到系统提示
            SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                    .skillRegistry(registry)
                    .build();

            // 3. Shell Hook：提供 Shell 命令执行（工作目录可指定，如当前工程目录）
            ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                    .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
                    .build();

            agentInfoDTO.setIsMemory(true);
            agentInfoDTO.setMemorySaver(new MemorySaver());
            agentInfoDTO.setHooks(List.of(skillsHook, shellHook));
            agentInfoDTO.setTools(Collections.singletonList(PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION)));
            reactAgent = (ReactAgent) agent.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
            // 5. 测试调用！
            // 测试1：让Agent介绍自己的技能
//            AssistantMessage result1 = reactAgent.call("请介绍你拥有的所有技能");
//            System.out.println("=== 技能列表 ===");
//            System.out.println(result1.getText());

            // 测试2：调用frontend-design技能
            AssistantMessage result2 = reactAgent.call("帮我设计一个响应式的登录页面 直接用html就能看到效果 ，要求高端大气上档次");
            System.out.println("\n=== 前端设计结果 ===");
            System.out.println(result2.getText());

            // 测试3：调用pdf技能
//            AssistantMessage result3 = reactAgent.call("帮我提取并总结这个PDF文件的核心内容");
//            System.out.println("\n=== PDF处理结果 ===");
//            System.out.println(result3.getText());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void agent25() {
        // 1. 配置检查点
        MemorySaver memorySaver = new MemorySaver();

        // 2. 创建人工介入Hook
        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("getUserInfoAndSendEmail", ToolConfig.builder()
                        .description("请确认发送邮件功能")
                        .build())
                .build();
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder().toolObjects(new SendEmailTools()).build().getToolCallbacks();
        // 3. 创建Agent
        ReactAgent agent = ReactAgent.builder()
                .name("email")
                .model(chatModel)
                .tools(toolCallbacks)
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "user-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 4. 第一次调用 - 触发中断
        System.out.println("=== 第一次调用：期望中断 ===");
        Optional<NodeOutput> result = null;
        try {
            result = agent.invokeAndGetOutput(
                    "帮我给 张三发一封清明节的邮件",
                    config
            );
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

        // 5. 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata interruptionMetadata) {
            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 6. 模拟人工决策（这里选择批准）
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            toolFeedbacks.forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 7. 第二次调用 - 使用人工反馈恢复执行
            System.out.println(" === 第二次调用：使用批准决策恢复 ===");
            RunnableConfig resumeConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                    .build();

            Optional<NodeOutput> finalResult;
            try {
                finalResult = agent.invokeAndGetOutput("", resumeConfig);
            } catch (GraphRunnerException e) {
                throw new RuntimeException(e);
            }

            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                System.out.println("最终结果: " + finalResult.get());
            }
        }
    }

    @Test
    void agent26() {
        // 定义子Agent的输入Schema（标准 JSON Schema 格式）
        String writerInputSchema = """
                {
                    "type": "object",
                    "properties": {
                        "topic": {
                            "type": "string"
                        },
                        "wordCount": {
                            "type": "integer"
                        },
                        "style": {
                            "type": "string"
                        }
                    },
                    "required": ["topic", "wordCount", "style"]
                }
                """;
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        ReactAgent writerAgent = ReactAgent.builder()
                .name("structured_writer_agent")
                .model(chatModel)
                .description("根据结构化输入写文章")
                .instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章。")
                .inputSchema(writerInputSchema)
                .build();

        ReactAgent coordinatorAgent = ReactAgent.builder()
                .name("coordinator_agent")
                .model(chatModel)
                .instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
                .tools(AgentTool.getFunctionToolCallback(writerAgent))
                .build();

        try {
            Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于春天的散文，大约150字");
            if (result.isPresent()) {
                OverAllState state = result.get();

                // 访问消息历史
                Optional<Object> messages = state.value("messages");
                List<Message> messageList = (List<Message>) messages.get();
                messageList.forEach(System.out::println);

                // 访问自定义状态
                Optional<Object> customData = state.value("custom_key");

                System.out.println("完整状态：" + state);
            }
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);


        }
    }

    @Test
    void agent27() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 创建写作Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("专门负责创作文章和内容生成")
                .instruction("你是一个专业作家，擅长各类文章创作。")
                .build();

        // 创建翻译Agent
        ReactAgent translatorAgent = ReactAgent.builder()
                .name("translator_agent")
                .model(chatModel)
                .description("专门负责文本翻译工作")
                .instruction("你是一个专业翻译，能够准确翻译多种语言。")
                .build();

// 创建总结Agent
        ReactAgent summarizerAgent = ReactAgent.builder()
                .name("summarizer_agent")
                .model(chatModel)
                .description("专门负责内容总结和提炼")
                .instruction("你是一个内容总结专家，擅长提炼关键信息。")
                .build();

// 创建主Agent，集成多个工具
        ReactAgent multiToolAgent = ReactAgent.builder()
                .name("multi_tool_coordinator")
                .model(chatModel)
                .instruction("你可以访问多个专业工具：写作、翻译和总结。" +
                        "根据用户需求选择合适的工具来完成任务。")
                .tools(
                        AgentTool.getFunctionToolCallback(writerAgent),
                        AgentTool.getFunctionToolCallback(translatorAgent),
                        AgentTool.getFunctionToolCallback(summarizerAgent)
                )
                .build();
        try {
            Optional<OverAllState> result = multiToolAgent.invoke("请写一篇关于AI的文章，然后翻译成英文，最后给出摘要");
            if (result.isPresent()) {
                OverAllState state = result.get();

                // 访问消息历史
                Optional<Object> messages = state.value("messages");
                List<Message> messageList = (List<Message>) messages.get();
                messageList.forEach(System.out::println);

                // 访问自定义状态
                Optional<Object> customData = state.value("custom_key");

                System.out.println("完整状态：" + state);
            }
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);


        }
    }

    @Test
    void agent28() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 创建专门的数据分析 Agent
        ReactAgent analysisAgent = ReactAgent.builder()
                .name("data_analyzer")
                .model(chatModel)
                .instruction("你是一个数据分析专家，负责分析数据并提供洞察，请分析以下输入数据： {input}")
                .outputKey("analysis_result")
                .build();

        // 创建报告生成 Agent
        ReactAgent reportAgent = ReactAgent.builder()
                .name("report_generator")
                .model(chatModel)
                .instruction("你是一个报告生成专家，负责将分析结果 {analysis_result} 转化为专业报告")
                .outputKey("final_report")
                .build();

        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            return strategies;
        };

        // 构建包含 Agent 的工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 将 Agent 作为 SubGraph Node 添加
        try {
            workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
                    true,                     // includeContents: 是否传递父图的消息历史
                    false                     // returnReasoningContents: 是否返回推理过程
            ));

            workflow.addNode(reportAgent.name(), reportAgent.asNode(
                    true,
                    false
            ));

            // 定义流程
            workflow.addEdge(StateGraph.START, analysisAgent.name());
            workflow.addEdge(analysisAgent.name(), reportAgent.name());
            workflow.addEdge(reportAgent.name(), StateGraph.END);

            // 编译并执行工作流
            CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
            NodeOutput lastOutput = compiledGraph.stream(
                            Map.of("input", "2025年全年销量100亿，毛利率 23%，净利率 13%。2024年全年销量80亿，毛利率 20%，净利率 8%。"))
                    .doOnNext(output -> {
                        if (output instanceof StreamingOutput<?> streamingOutput) {
                            System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
                        }
                    })
                    .blockLast();

            System.out.println(" 最终结果，包含所有节点状态： " + lastOutput.state().data());
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void agent29() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 1. 创建信息收集 Agent
        ReactAgent researchAgent = ReactAgent.builder()
                .name("researcher")
                .model(chatModel)
                .instruction("你是一个研究专家，负责收集和整理相关信息，请研究主题： {input}")
                .outputKey("research_data")
                .enableLogging(true)
                .build();

        // 2. 创建数据分析 Agent
        ReactAgent analysisAgent = ReactAgent.builder()
                .name("analyst")
                .model(chatModel)
                .instruction("你是一个分析专家，负责深入分析关于主题  {input}  的研究数据。数据如下： { research_data} ")
                .outputKey("analysis_result")
                .enableLogging(true)
                .build();

        // 3. 创建总结 Agent
        ReactAgent summaryAgent = ReactAgent.builder()
                .name("summarizer")
                .model(chatModel)
                .instruction("你是一个总结专家，负责将分析结果提炼为简洁的结论，结果： {analysis_result} ")
                .outputKey("final_summary")
                .enableLogging(true)
                .build();

        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            return strategies;
        };

        // 4. 构建工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 添加 Agent 节点
        try {
            workflow.addNode(researchAgent.name(), researchAgent.asNode(
                    true,    // 包含历史消息
                    false    // 不返回推理过程
            ));

            workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
                    true,
                    false
            ));

            workflow.addNode(summaryAgent.name(), summaryAgent.asNode(
                    true,
                    true     // 返回完整推理过程
            ));

            // 定义顺序执行流程
            workflow.addEdge(StateGraph.START, researchAgent.name());
            workflow.addEdge(researchAgent.name(), analysisAgent.name());
            workflow.addEdge(analysisAgent.name(), summaryAgent.name());
            workflow.addEdge(summaryAgent.name(), StateGraph.END);

            // 编译并执行工作流
            CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
            NodeOutput finalOutput = compiledGraph.stream(Map.of("input", "帮我做一份关于AI Agent的研究报告"))
                    .doOnNext(output -> {
                        if (output instanceof StreamingOutput<?> streamingOutput) {
                            System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
                        }
                    })
                    .blockLast();

            System.out.println("多Agent研究工作流构建完成");
            System.out.println("最终输出: " + finalOutput.state().value("final_summary").orElse("无"));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void agent30() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();
        // 创建带有 RAG Hook 的 Agent
        ReactAgent ragAgent = ReactAgent.builder()
                .name("rag_agent")
                .model(chatModel)
                .hooks(new RAGMessagesHook((ObjectProvider<VectorStore>) vectorStore))
                .build();

        // 调用 Agent
        AssistantMessage response = null;
        try {
            response = ragAgent.call("终端报文返回dar=4 是什么意思 ");
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
        System.out.println("答案: " + response.getText());


    }
    @Test
    void agent31() {
        ChatModel chatModel = getChatModel.creatDashScopeChatModel();

        ReactAgent ragAgent = ReactAgent.builder()
                .name("rag_agent")
                .model(chatModel)
                .interceptors(new RAGModelInterceptor(vectorStore))
                .build();

// 调用 Agent
        AssistantMessage response = null;
        try {
            response = ragAgent.call("什么是基于应用连接的数据交换 ");
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
        System.out.println("答案: " + response.getText());


    }


}
