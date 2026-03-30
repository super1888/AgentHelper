package com.spring.quickstart.hooks;

import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;

/**
 * 限制模型调用次数的钩子
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/27
 */
public class ModelLimitHook {

    /**
     * @param runLimit     - 每个线程的运行次数限制 同一会话（跨多轮 run）的累计模型调用次数上限，用于控制整场对话总消耗
     * @param threadLimit  - 单次会话的默认执行线程数限制 单个 Agent 运行周期内的模型调用次数上限，防止单次任务调用爆炸
     * @param exitBehavior - 触发限制时的处理策略，可选 ERROR（抛异常）或 END（优雅终止）
     * @return
     */
    public ModelCallLimitHook getModelCallLimitHook(Integer runLimit, Integer threadLimit, ModelCallLimitHook.ExitBehavior exitBehavior) {
        return ModelCallLimitHook.builder().runLimit(runLimit).threadLimit(threadLimit).exitBehavior(exitBehavior).build();
    }

}
