package com.spring.ai.graph.support;

import com.alibaba.cloud.ai.graph.OverAllState;

import com.spring.ai.graph.stateKeys.ApprovalWorkflowStateKeys;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间线工具类。
 *
 * <p>为了让学习体验更直观，每个节点都会往时间线里写一条中文说明。后续你看接口返回值时，
 * 就能清楚知道 Graph 具体按什么顺序执行过哪些节点。</p>
 */
public final class ApprovalTimelineSupport {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ApprovalTimelineSupport() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> appendTimeline(OverAllState state, String message) {
        List<String> timeline = new ArrayList<>((List<String>) state.value(
                ApprovalWorkflowStateKeys.TIMELINE,
                new ArrayList<String>()));
        timeline.add("[" + LocalDateTime.now().format(FORMATTER) + "] " + message);
        return timeline;
    }
}
