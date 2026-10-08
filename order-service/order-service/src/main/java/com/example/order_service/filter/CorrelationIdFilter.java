package com.example.order_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract the ID passed by the API Gateway
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // 2. If missing (e.g., direct access bypassing Gateway), generate a fallback
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = "DIRECT-" + UUID.randomUUID().toString();
        }

        // 3. Put the ID into the Mapped Diagnostic Context (MDC)
        // MDC ensures this ID is automatically prepended to every log line printed by this specific thread
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. Always clean up the thread after the request finishes to prevent memory leaks
            MDC.remove(MDC_KEY);
        }
    }
}