package com.spring.ai.graph.config;

import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;
import java.util.Locale;
import java.util.UUID;

/**
 * 审批工作流 threadId 辅助类。
 *
 * <p>为了把 checkpoint 存储选择权交给用户，每次创建流程时都会把所选存储模式编码进 threadId。
 * 这样后续查询、审批恢复时只传 threadId，就能自动路由到对应的 saver。
 */
public final class ThreadIdSupport {

    private static final String SEPARATOR = ":";

    private ThreadIdSupport() {
    }

    public static String createThreadId(ApprovalWorkflowCheckpointModeEnum mode) {
        return normalizeMode(mode).name().toLowerCase(Locale.ROOT) + SEPARATOR + UUID.randomUUID();
    }

    public static ApprovalWorkflowCheckpointModeEnum resolveMode(String threadId) {
        if (threadId == null || !threadId.contains(SEPARATOR)) {
            return null;
        }
        String prefix = threadId.substring(0, threadId.indexOf(SEPARATOR));
        if (!org.springframework.util.StringUtils.hasText(prefix)) {
            return null;
        }
        try {
            return ApprovalWorkflowCheckpointModeEnum.valueOf(prefix.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static ApprovalWorkflowCheckpointModeEnum normalizeMode(ApprovalWorkflowCheckpointModeEnum mode) {
        return mode == null ? ApprovalWorkflowCheckpointModeEnum.MEMORY : mode;
    }
}
