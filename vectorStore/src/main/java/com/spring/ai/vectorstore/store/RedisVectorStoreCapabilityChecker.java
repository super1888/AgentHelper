package com.spring.ai.vectorstore.store;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Redis 向量库能力检测器。
 * 用于在写入或检索前检查 Redis 是否具备 RedisJSON 和 RediSearch 能力。
 * @author zhuoqi
 */
@Component
@Slf4j
public class RedisVectorStoreCapabilityChecker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final AtomicReference<Boolean> capabilityReady = new AtomicReference<>(false);

    /**
     * 校验 Redis 是否支持 Spring AI Redis 向量存储所需命令。
     */
    public void ensureReady() {
        if (Boolean.TRUE.equals(capabilityReady.get())) {
            return;
        }

        try {
            Boolean jsonReady = stringRedisTemplate.execute(this::supportsJsonSet);
            Boolean searchReady = stringRedisTemplate.execute(this::supportsFtSearch);
            if (!Boolean.TRUE.equals(jsonReady) || !Boolean.TRUE.equals(searchReady)) {
                throw new VectorStoreException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "当前 Redis 实例不支持 Redis Stack 命令，请使用 Redis Stack 或安装 RedisJSON 与 RediSearch 模块。");
            }
            capabilityReady.compareAndSet(false, true);
        }
        catch (VectorStoreException exception) {
            throw exception;
        }
        catch (DataAccessException exception) {
            log.error("Failed to verify Redis vector store capabilities", exception);
            throw new VectorStoreException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "校验 Redis 向量库能力失败，请检查 Redis 连接状态及模块安装情况。",
                    exception);
        }
    }

    private Boolean supportsJsonSet(RedisConnection connection) {
        Object result = connection.execute(
                "COMMAND",
                toBytes("INFO"),
                toBytes("JSON.SET"));
        return hasCommandMetadata(result);
    }

    private Boolean supportsFtSearch(RedisConnection connection) {
        Object result = connection.execute(
                "COMMAND",
                toBytes("INFO"),
                toBytes("FT.SEARCH"));
        return hasCommandMetadata(result);
    }

    private byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private boolean hasCommandMetadata(Object result) {
        if (result == null) {
            return false;
        }
        if (result instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }
}
