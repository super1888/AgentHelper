package com.spring.ai.agent.application.service.agentscope;

import com.spring.ai.agent.domain.request.AgentScopeReactChatRequest;
import com.spring.ai.agent.domain.response.AgentScopeReactChatResponse;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.ModelProviderEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AgentScope ReAct Agent 执行服务。
 * 负责组装模型、工具集和执行边界，提供企业级 ReAct 调用入口。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Service
public class AgentScopeReactAgentService {

    private static final String TOOL_CALCULATE = "calculate";

    private static final String TOOL_CURRENT_TIME = "get_current_time";

    private static final int DEFAULT_MAX_ITERS = 6;

    private static final int MIN_MAX_ITERS = 1;

    private static final int MAX_MAX_ITERS = 12;

    private static final String DEFAULT_SYSTEM_PROMPT = "你是企业级 ReAct 智能体。请先判断是否需要工具，"
            + "需要时再调用工具，最后用中文给出简洁、可执行的答案。";

    @Resource
    private CoreApplicationManager coreApplicationManager;

    /**
     * 执行 AgentScope ReAct 对话。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    public AgentScopeReactChatResponse chat(AgentScopeReactChatRequest request) {
        validateRequest(request);
        ChatModelRequest modelRequest = coreApplicationManager.createChatModelRequestByModelCode(request.getModelCode());
        validateProvider(modelRequest);
        int maxIters = resolveMaxIters(request.getMaxIters());
        List<String> enabledTools = resolveEnabledTools(request.getEnabledTools());
        long start = System.currentTimeMillis();

        ReActAgent agent = ReActAgent.builder()
                .name("AgentHelper-AgentScope-ReAct")
                .sysPrompt(resolveSystemPrompt(request.getSystemPrompt()))
                .model(buildDashScopeModel(modelRequest))
                .toolkit(buildToolkit(enabledTools))
                .maxIters(maxIters)
                .build();

        Msg response = agent.call(Msg.builder()
                .textContent(request.getUserPrompt().trim())
                .build()).block();

        return AgentScopeReactChatResponse.builder()
                .modelCode(request.getModelCode().trim())
                .userPrompt(request.getUserPrompt().trim())
                .finalAnswer(response == null ? null : response.getTextContent())
                .enabledTools(enabledTools)
                .maxIters(maxIters)
                .costMs(System.currentTimeMillis() - start)
                .build();
    }

    private void validateRequest(AgentScopeReactChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "AgentScope ReAct 请求不能为空");
        }
        if (!StringUtils.hasText(request.getModelCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型");
        }
        if (!StringUtils.hasText(request.getUserPrompt())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请输入用户问题或任务");
        }
    }

    private void validateProvider(ChatModelRequest modelRequest) {
        ModelProviderEnum provider = ModelProviderEnum.fromValue(modelRequest.getProvider());
        if (provider != ModelProviderEnum.DASHSCOPE) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "AgentScope ReAct 当前仅支持 DashScope 模型配置");
        }
    }

    private DashScopeChatModel buildDashScopeModel(ChatModelRequest request) {
        return DashScopeChatModel.builder()
                .apiKey(request.getApiKey())
                .modelName(request.getModel())
                .build();
    }

    private Toolkit buildToolkit(List<String> enabledTools) {
        Toolkit toolkit = new Toolkit();
        if (enabledTools.contains(TOOL_CALCULATE)) {
            toolkit.registerTool(new CalculatorTool());
        }
        if (enabledTools.contains(TOOL_CURRENT_TIME)) {
            toolkit.registerTool(new TimeTool());
        }
        return toolkit;
    }

    private List<String> resolveEnabledTools(List<String> enabledTools) {
        if (enabledTools == null || enabledTools.isEmpty()) {
            return List.of(TOOL_CALCULATE, TOOL_CURRENT_TIME);
        }
        Set<String> resolved = new LinkedHashSet<>();
        for (String enabledTool : enabledTools) {
            if (!StringUtils.hasText(enabledTool)) {
                continue;
            }
            String toolName = enabledTool.trim();
            if (!TOOL_CALCULATE.equals(toolName) && !TOOL_CURRENT_TIME.equals(toolName)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不支持的 AgentScope 工具：" + toolName);
            }
            resolved.add(toolName);
        }
        return new ArrayList<>(resolved);
    }

    private int resolveMaxIters(Integer maxIters) {
        if (maxIters == null) {
            return DEFAULT_MAX_ITERS;
        }
        if (maxIters < MIN_MAX_ITERS || maxIters > MAX_MAX_ITERS) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "最大推理轮次必须在 1 到 12 之间");
        }
        return maxIters;
    }

    private String resolveSystemPrompt(String systemPrompt) {
        return StringUtils.hasText(systemPrompt) ? systemPrompt.trim() : DEFAULT_SYSTEM_PROMPT;
    }

    private static class TimeTool {

        @Tool(name = TOOL_CURRENT_TIME, description = "查询指定时区的当前时间")
        public String getCurrentTime(
                @ToolParam(name = "timezone", description = "时区名称，例如 Asia/Shanghai") String timezone
        ) {
            try {
                ZoneId zoneId = ZoneId.of(timezone);
                String time = LocalDateTime.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return timezone + " 当前时间：" + time;
            } catch (Exception ex) {
                return "时区不合法，请使用 Asia/Shanghai、Asia/Tokyo、America/New_York 等标准时区名称";
            }
        }
    }

    private static class CalculatorTool {

        @Tool(name = TOOL_CALCULATE, description = "执行两个数字之间的简单四则运算，支持 +、-、*、/")
        public String calculate(
                @ToolParam(name = "expression", description = "数学表达式，例如 12 + 30") String expression
        ) {
            if (expression == null || expression.isBlank()) {
                return "表达式不能为空";
            }
            String normalized = expression.replaceAll("\\s+", "");
            try {
                int operatorIndex = findOperatorIndex(normalized);
                if (operatorIndex <= 0 || operatorIndex >= normalized.length() - 1) {
                    return "仅支持两个数字之间的 +、-、*、/ 运算";
                }
                double left = Double.parseDouble(normalized.substring(0, operatorIndex));
                double right = Double.parseDouble(normalized.substring(operatorIndex + 1));
                char operator = normalized.charAt(operatorIndex);
                double result = switch (operator) {
                    case '+' -> left + right;
                    case '-' -> left - right;
                    case '*' -> left * right;
                    case '/' -> right == 0 ? Double.NaN : left / right;
                    default -> throw new IllegalArgumentException("unsupported operator");
                };
                if (Double.isNaN(result)) {
                    return "除数不能为 0";
                }
                return normalized + " = " + result;
            } catch (Exception ex) {
                return "表达式格式错误，示例：12 + 30";
            }
        }

        private int findOperatorIndex(String expression) {
            for (int index = 1; index < expression.length(); index++) {
                char current = expression.charAt(index);
                if (current == '+' || current == '-' || current == '*' || current == '/') {
                    return index;
                }
            }
            return -1;
        }
    }
}
