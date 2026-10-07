package com.example.order_service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    // How long to remember the idempotency key (e.g., 24 hours)
    private static final long TTL_HOURS = 24;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a lock for the given key.
     * Prevents two identical requests from processing simultaneously.
     */
    public boolean acquireLock(String idempotencyKey) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + idempotencyKey, "PROCESSING", 10, TimeUnit.SECONDS);
        return acquired != null && acquired;
    }

    public void releaseLock(String idempotencyKey) {
        redisTemplate.delete("lock:" + idempotencyKey);
    }

    public boolean isAlreadyProcessed(String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("idemp:" + idempotencyKey));
    }

    public Object getCachedResponse(String idempotencyKey) {
        return redisTemplate.opsForValue().get("idemp:" + idempotencyKey);
    }

    public void cacheResponse(String idempotencyKey, Object response) {
        redisTemplate.opsForValue().set("idemp:" + idempotencyKey, response, TTL_HOURS, TimeUnit.HOURS);
    }
}