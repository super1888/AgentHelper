package com.spring.ai.interceptors.factory;

import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.spring.ai.interceptors.domain.dto.TodoListInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolEmulatorInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolRetryInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolSelectionInterceptorDTO;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 拦截器工厂
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Service
public class InterceptorsFactory {

    /**
     * 自动重试失败的工具调用，具有可配置的指数退避。
     * <p>
     * 适用场景：
     * <p>
     * 处理外部 API 调用中的瞬态故障； 提高依赖网络的工具的可靠性； 构建优雅处理临时错误的弹性 Agent。 创建 ToolRetryInterceptor
     */
    public ToolRetryInterceptor createToolRetryInterceptor(ToolRetryInterceptorDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("ToolRetryInterceptorDTO 不能为空");
        }

        return ToolRetryInterceptor.builder()
                .maxRetries(dto.getMaxRetries())
                .toolNames(dto.getToolNames())
                .retryOn(dto.getRetryOn())
                .onFailure(dto.getOnFailure())
                .errorFormatter(dto.getErrorFormatter())
                .backoffFactor(dto.getBackoffFactor())
                .initialDelay(dto.getInitialDelayMs())
                .maxDelay(dto.getMaxDelayMs())
                .jitter(dto.isJitter())
                .build();
    }

    /**
     *  * 任务规划拦截器 DTO（TodoListInterceptor）
     *  * 让 Agent 先拆解复杂任务 → 生成待办事项 → 按步骤执行
     *  * 官方内置默认 systemPrompt + toolDescription，支持自定义覆盖
     */
    /**
     * 创建任务规划拦截器
     */
    public TodoListInterceptor createTodoListInterceptor(TodoListInterceptorDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TodoListInterceptorDTO 不能为空");
        }

        // 完全匹配你给的官方 Builder
        return new TodoListInterceptor.Builder()
                .systemPrompt(dto.getSystemPrompt())
                .toolDescription(dto.getToolDescription())
                .build();

    }

    /**
     * 1. 作用 ToolSelectionInterceptor 是 Spring Cloud AI 智能体（Agent）的工具选择拦截器，核心功能： 让 AI 大模型自动筛选最相关的工具来回 答用户问题 避免一次性加载所有工具导致 token
     * 浪费、推理变慢支持固定必须包含的工具、限制最大选择工具数量 用于 Agent 执行流程中动态裁剪可用工具列表 2. 使用场景 Agent 有大量工具（10+），但每次只需要用 2~3 个 需要强制某些工具永远可用 （如查询、记忆工具）
     * 控制工具选择数量，提升回答速度与准确性多工具智能体自动化流程优化
     *
     * @param dto
     * @return
     */
    public ToolSelectionInterceptor createToolSelectionInterceptor(ToolSelectionInterceptorDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TodoListInterceptorDTO 不能为空");
        }

        // 1. 获取Builder
        ToolSelectionInterceptor.Builder builder = new ToolSelectionInterceptor.Builder();

        // 2. 必填：设置选择模型
        builder.selectionModel(dto.getSelectionModel());

        // 3. 可选：设置提示词
        if (dto.getSystemPrompt() != null) {
            builder.systemPrompt(dto.getSystemPrompt());
        }

        // 4. 可选：设置最大工具数量
        if (dto.getMaxTools() != null) {
            builder.maxTools(dto.getMaxTools());
        }

        // 5. 可选：设置永远包含的工具
        Set<String> alwaysInclude = dto.getAlwaysInclude();
        if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
            builder.alwaysInclude(alwaysInclude);
        }

        // 6. 构建并返回
        return builder.build();

    }

    /**
     * 1. 作用 ToolEmulatorInterceptor 是 AI Agent 工具模拟器拦截器，核心功能： 不真实调用第三方工具 / 接口，让 AI 大模型模拟工具返回结果 用于测试、调试、演示 Agent 流程 支持模拟指定工具 / 模拟所有工具
     * 自定义模拟提示词模板，控制模拟结果风格 2. 使用场景 工具接口未开发完成，需要先调试 Agent 流程 单元测试 / 集成测试，避免真实调用外部服务 演示环境、离线环境运行 Agent 快速验证工具调用逻辑是否正确
     *
     * @param dto
     * @return
     */
    public ToolEmulatorInterceptor createToolEmulatorInterceptor(ToolEmulatorInterceptorDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TodoListInterceptorDTO 不能为空");
        }

        ToolEmulatorInterceptor.Builder builder = new ToolEmulatorInterceptor.Builder();

        // 必填：设置模拟模型
        if (dto.getEmulatorModel() == null) {
            throw new IllegalStateException("emulatorModel 不能为空");
        }
        builder.model(dto.getEmulatorModel());

        // 可选：设置需要模拟的工具列表
        if (dto.getToolsToEmulate() != null && !dto.getToolsToEmulate().isEmpty()) {
            builder.addTools(dto.getToolsToEmulate());
        }

        // 可选：是否模拟所有工具
        builder.emulateAllTools(dto.isEmulateAll());

        // 可选：自定义提示模板
        if (dto.getPromptTemplate() != null && !dto.getPromptTemplate().isBlank()) {
            builder.promptTemplate(dto.getPromptTemplate());
        }

        return builder.build();

    }

}
