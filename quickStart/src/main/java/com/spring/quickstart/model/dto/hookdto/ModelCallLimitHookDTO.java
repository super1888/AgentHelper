package com.spring.quickstart.model.dto.hookdto;

import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook.ExitBehavior;
import lombok.Builder;
import lombok.Data;

/**
 * 限制模型调用次数以防止无限循环或过度成本。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class ModelCallLimitHookDTO {

    /**
     * 单次运行（单次 agent.invoke()）最大模型调用次数 为空则不限制单次调用次数 示例：5 → 本轮执行最多调用模型 5 次
     */
    private Integer runLimit;

    /**
     * 整个会话（Thread）累计最大模型调用次数 为空则不限制会话级调用次数 示例：20 → 整个对话生命周期最多调用模型 20 次
     */
    private Integer threadLimit;

    /**
     * 超限后的退出行为 - ERROR：抛出 ModelCallLimitExceededException，终止执行（便于监控告警） - END：添加超限提示消息，优雅结束 Agent 执行，不抛异常 默认：ERROR
     */
    private ModelCallLimitHook.ExitBehavior exitBehavior;
}
