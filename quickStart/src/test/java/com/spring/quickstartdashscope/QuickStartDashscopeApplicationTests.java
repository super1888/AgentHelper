package com.spring.quickstartdashscope;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.spring.quickstartdashscope.Hooks.LoggingHook;
import com.spring.quickstartdashscope.Hooks.MessageTrimmingHook;
import com.spring.quickstartdashscope.agent.Agent;
import com.spring.quickstartdashscope.chatModel.DashScope;
import com.spring.quickstartdashscope.interceptor.ToolErrorInterceptor;
import com.spring.quickstartdashscope.model.dto.AgentInfoDTO;
import com.spring.quickstartdashscope.model.result.PoemOutputResult;
import com.spring.quickstartdashscope.model.result.TextAnalysisResult;
import com.spring.quickstartdashscope.tools.CalculatorTools;
import com.spring.quickstartdashscope.tools.LocalTools;
import com.spring.quickstartdashscope.tools.WeatherTools;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class QuickStartDashscopeApplicationTests {

    private AgentInfoDTO getAgent() {
        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("学习agent").model(dashScope.getSeniorModel())
                .tools(Arrays.asList(new CalculatorTools(), new LocalTools(), new WeatherTools()))
                .interceptors(Arrays.asList(new ToolErrorInterceptor(), new ToolErrorInterceptor()))
                .instruction("你是一个学习助手，请根据用户的问题，使用工具回答用户的问题").build();
        return agentInfoDTO;
    }


    @Resource
    private Agent agent;

    @Resource
    private DashScope dashScope;

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

        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder().agentId(1L).agentName("学习agent").model(dashScope.getSeniorModel())
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
     *
     * AGENT_MODEL_STREAMING	模型推理的流式增量内容
     * AGENT_MODEL_FINISHED	模型推理完成，可获取全量内容
     * AGENT_TOOL_STREAMING	工具调用的流式增量内容
     * AGENT_TOOL_FINISHED	工具调用完成
     * AGENT_HOOK_STREAMING	Hook 节点的流式增量内容
     * AGENT_HOOK_FINISHED	Hook 节点完成
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


}
