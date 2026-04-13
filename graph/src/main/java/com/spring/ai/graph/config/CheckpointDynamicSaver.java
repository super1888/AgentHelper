package com.spring.ai.graph.config;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * 动态 checkpoint 保存器。
 *
 * <p>它本身不是具体的存储实现，而是一个路由层：
 * 根据当前配置把 Graph 的 checkpoint 操作转发到 MemorySaver 或 RedisSaver。
 *
 * <p>这样做的价值是：
 * 1. Graph compile 配置不需要关心具体实现 2. 以后切换存储方式时，业务代码不需要改 3. 配置中心切换时，后续新的 checkpoint 操作会自动走新实现
 *
 * <p>注意：如果运行中从 MEMORY 切到 REDIS，旧的内存态不会自动迁移到 Redis，
 * 这是存储切换本身的语义边界，不是路由器能无损解决的问题。
 */
@Slf4j
public class CheckpointDynamicSaver implements BaseCheckpointSaver {

    private final CheckpointProperties properties;
    private final BaseCheckpointSaver memorySaver;
    private final BaseCheckpointSaver redisSaver;

    public CheckpointDynamicSaver(
            CheckpointProperties properties,
            BaseCheckpointSaver memorySaver,
            BaseCheckpointSaver redisSaver) {
        this.properties = properties;
        this.memorySaver = memorySaver;
        this.redisSaver = redisSaver;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        return currentSaver(config).list(config);
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        return currentSaver(config).get(config);
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        return currentSaver(config).put(config, checkpoint);
    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        return currentSaver(config).release(config);
    }

    private BaseCheckpointSaver currentSaver(RunnableConfig config) {
        ApprovalWorkflowCheckpointModeEnum mode = resolveMode(config);
        if (mode == ApprovalWorkflowCheckpointModeEnum.REDIS) {
            log.debug("审批工作流 checkpoint 当前使用 RedisSaver");
            return redisSaver;
        }
        log.debug("审批工作流 checkpoint 当前使用 MemorySaver");
        return memorySaver;
    }

    private ApprovalWorkflowCheckpointModeEnum resolveMode(RunnableConfig config) {
        if (config != null) {
            ApprovalWorkflowCheckpointModeEnum threadMode = config.threadId()
                    .map(ThreadIdSupport::resolveMode)
                    .orElse(null);
            if (threadMode != null) {
                return threadMode;
            }
        }
        return ThreadIdSupport.normalizeMode(properties.getType());
    }
}
