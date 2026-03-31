package com.spring.quickstart;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.spring.quickstart.agent.Agent;
import com.spring.quickstart.chatModel.GetChatModel;
import com.spring.quickstart.chatModel.GetDashScopeChatModel;
import com.spring.quickstart.hooks.LoggingHook;
import com.spring.quickstart.hooks.MessageTrimmingHook;
import com.spring.quickstart.interceptor.ToolErrorInterceptor;
import com.spring.quickstart.model.dto.AgentInfoDTO;
import com.spring.quickstart.model.dto.AssistantMessageDTO;
import com.spring.quickstart.model.result.PoemOutputResult;
import com.spring.quickstart.model.result.TextAnalysisResult;
import com.spring.quickstart.tools.CalculatorTools;
import com.spring.quickstart.tools.GetUserInfoTools;
import com.spring.quickstart.tools.LocalTools;
import com.spring.quickstart.tools.SendEmailTools;
import com.spring.quickstart.tools.WeatherTools;
import com.spring.quickstart.userMessage.MessageType;
import jakarta.annotation.Resource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.ai.content.Media;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

@SpringBootTest
class QuickStartApplicationTests {

    private AgentInfoDTO getAgent() {
        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("学习agent").model(getDashScopeChatModel.getSeniorModel())
                .tools(Arrays.asList(new CalculatorTools(), new LocalTools(), new WeatherTools()))
                .interceptors(Arrays.asList(new ToolErrorInterceptor(), new ToolErrorInterceptor()))
                .instruction("你是一个学习助手，请根据用户的问题，使用工具回答用户的问题").build();
        return agentInfoDTO;
    }


    @Resource
    private Agent agent;

    @Resource
    private GetDashScopeChatModel getDashScopeChatModel;

    @Resource
    private GetChatModel getChatModel;

    @Test
    void Agent1() {
        ReactAgent reactAgent = agent.creatAgent();

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
        ReactAgent reactAgent = agent.creatAgentTool();
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
        ReactAgent reactAgent = agent.creatAgentPrompt();
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
                .tools(Arrays.asList(new CalculatorTools(), new LocalTools(), new WeatherTools()))
                .interceptors(Arrays.asList(new ToolErrorInterceptor(), new ToolErrorInterceptor()))
                .instruction("你是一个学习助手，请根据用户的问题，使用工具回答用户的问题").build();
        ReactAgent reactAgent = null;
        try {
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            reactAgent = agent.customAgent(agentInfoDTO);


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
            ReactAgent reactAgent = agent.customAgent(agentInfoDTO);
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
            ReactAgent reactAgent = agent.customAgent(agentInfoDTO);
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


}
