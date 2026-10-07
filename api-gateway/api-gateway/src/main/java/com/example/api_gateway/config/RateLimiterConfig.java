package com.example.api_gateway.config;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the client's IP address to use as the Redis tracking key.
     * This ensures User A's traffic does not consume User B's rate limit quota.
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress()
                        .getHostAddress()
        );
    }
}