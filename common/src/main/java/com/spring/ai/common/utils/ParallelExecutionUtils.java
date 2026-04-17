package com.spring.ai.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 并行执行工具类。
 * @author zhuoqi
 */
public final class ParallelExecutionUtils {

    private ParallelExecutionUtils() {
    }

    public static <T> List<List<T>> partition(List<T> source, int batchSize) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("批处理大小必须大于 0");
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < source.size(); i += batchSize) {
            partitions.add(source.subList(i, Math.min(i + batchSize, source.size())));
        }
        return partitions;
    }

    public static <T> void parallelConsumeBatches(List<List<T>> batches, Executor executor, Consumer<List<T>> consumer) {
        Objects.requireNonNull(batches, "batches 不能为空");
        Objects.requireNonNull(executor, "executor 不能为空");
        Objects.requireNonNull(consumer, "consumer 不能为空");

        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> consumer.accept(batch), executor))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
}
