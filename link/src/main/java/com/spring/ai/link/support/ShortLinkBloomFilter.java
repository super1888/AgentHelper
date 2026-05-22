package com.spring.ai.link.support;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 简易 Redis 布隆过滤器，用于快速判断短码是否可能存在。
 */
@Component
public class ShortLinkBloomFilter {

    private static final String BLOOM_KEY = "short-link:bloom:code";
    private static final long BIT_SIZE = 10_000_019L;
    private static final int HASH_COUNT = 5;

    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    public ShortLinkBloomFilter(org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void add(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (int i = 0; i < HASH_COUNT; i++) {
            stringRedisTemplate.opsForValue().setBit(BLOOM_KEY, hash(value, i), true);
        }
    }

    public boolean mightContain(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < HASH_COUNT; i++) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(BLOOM_KEY, hash(value, i));
            if (!Boolean.TRUE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    private long hash(String value, int seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((seed + ":" + value).getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes).mod(BigInteger.valueOf(BIT_SIZE)).longValue();
        } catch (NoSuchAlgorithmException ex) {
            return Math.floorMod((value + seed).hashCode(), BIT_SIZE);
        }
    }
}
