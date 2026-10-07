package com.example.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class LoggingAndCorrelationFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String incomingId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        // Ensure correlationId is assigned exactly once so it is effectively final
        final String correlationId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingId;

        // Mutate exchange into a separate, final reference
        final ServerWebExchange finalExchange = exchange.mutate()
                .request(builder -> builder.header(CORRELATION_ID_HEADER, correlationId))
                .build();

        log.info("🌐 [GATEWAY] Incoming Request: {} {} | Correlation-ID: {}",
                request.getMethod(), request.getURI().getPath(), correlationId);

        // Expression lambda (no curly braces) to avoid the IDE warning
        return chain.filter(finalExchange)
                .then(Mono.fromRunnable(() -> log.info("✅ [GATEWAY] Outgoing Response: Status {} | Correlation-ID: {}",
                        finalExchange.getResponse().getStatusCode(), correlationId)));
    }

    @Override
    public int getOrder() {
        return -1; // Highest priority to execute first
    }
}