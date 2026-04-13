package com.spring.quickstart;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import com.spring.ai.a2a.provider.RemoteAgent;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/12
 */

@SpringBootTest(classes = QuickStartApplication.class)
public class a2aTests {
    @Resource
    RemoteAgent remoteAgent;

    @Test
    void Agent1() {
        A2aRemoteAgent remote = remoteAgent.callRemoteAgent();
        // 远程调用
        Optional<OverAllState> result = null;
        try {
            result = remote.invoke("请根据季度数据给出同比与环比分析概要。");
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }

        result.ifPresent(state -> {
            System.out.println("调用成功: " + state.value("output"));
        });
    }
}
