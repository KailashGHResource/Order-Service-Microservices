package com.example.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
public class RedisLockConfig {

    private static final String LOCK_REGISTRY_KEY = "order-service-locks";

    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        // Locks will automatically expire after 10 seconds to prevent deadlocks if a node crashes
        return new RedisLockRegistry(redisConnectionFactory, LOCK_REGISTRY_KEY, 10000L);
    }
}