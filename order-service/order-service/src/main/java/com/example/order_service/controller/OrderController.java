package com.example.order_service.controller;

import com.example.order_service.application.OrderCommandService;
import com.example.order_service.application.OrderQueryService;
import com.example.order_service.application.OrderEventReplayService;
import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.dto.OrderRequestDto;
import com.example.order_service.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService commandService;
    private final OrderQueryService queryService;
    private final OrderEventReplayService replayService;
    private final IdempotencyService idempotencyService; // Injected automatically by Lombok

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderRequestDto request) {

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            Long orderId = commandService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
        }

        // 2. Check if we already processed this request
        if (idempotencyService.isAlreadyProcessed(idempotencyKey)) {
            // Return the cached successful response immediately
            Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
            return ResponseEntity.ok(cachedResponse);
        }

        // 3. Prevent concurrent duplicate requests from race conditions
        if (!idempotencyService.acquireLock(idempotencyKey)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A request with this Idempotency-Key is currently being processed.");
        }

        try {
            // 4. Process the business logic normally
            Long orderId = commandService.createOrder(request);

            // 5. Cache the final result
            idempotencyService.cacheResponse(idempotencyKey, orderId);

            return ResponseEntity.status(HttpStatus.CREATED).body(orderId);

        } finally {
            // 6. Always release the lock so future retries can hit the cache check
            idempotencyService.releaseLock(idempotencyKey);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<Order>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.getOrders(status, pageable));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable Long id) {
        commandService.cancelOrder(id);
        return ResponseEntity.ok("Order cancellation event appended to store for ID: " + id);
    }

    @GetMapping("/{id}/replay")
    public ResponseEntity<Order> replayOrderEvents(@PathVariable String id) {
        return ResponseEntity.ok(replayService.reconstructOrderState(id));
    }
}