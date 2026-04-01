package com.spring.ai.interceptors.domain.dto;

import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor.OnFailureBehavior;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Data;

/**
 * 工具调用失败 / 超时 / 抛异常时自动重试，提升稳定性。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class ToolRetryInterceptorDTO {

    /**
     * 最大重试次数 默认：2
     */
    private int maxRetries;

    /**
     * 需要重试的工具名称集合 为空 = 对所有工具生效
     */
    private Set<String> toolNames;

    /**
     * 异常重试条件（Predicate） 根据异常类型判断是否需要重试 默认：所有异常都重试
     */
    private Predicate<Exception> retryOn;

    /**
     * 达到最大重试次数后的失败行为
     */
    private OnFailureBehavior onFailure;

    /**
     * 异常信息格式化函数 自定义异常提示文案
     */
    private Function<Exception, String> errorFormatter;

    /**
     * 退避因子（指数退避系数） 默认：0.0（无退避，固定间隔）
     */
    private double backoffFactor;

    /**
     * 初始重试延迟（毫秒） 默认：100ms
     */
    private long initialDelayMs;

    /**
     * 最大延迟（毫秒） 默认：3000ms
     */
    private long maxDelayMs;

    /**
     * 是否启用抖动（随机延迟） 默认：false
     */
    private boolean jitter;

}
