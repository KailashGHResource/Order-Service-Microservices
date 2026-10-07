package com.example.order_service.controller;

import com.example.order_service.application.OrderCommandService;
import com.example.order_service.application.OrderQueryService;
import com.example.order_service.application.OrderEventReplayService;
import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.dto.OrderRequestDto;
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
    private final OrderEventReplayService replayService; // Added for Event Replay

    @PostMapping
    public ResponseEntity<Long> createOrder(@Valid @RequestBody OrderRequestDto request) {
        Long orderId = commandService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
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
        // Uncommented this so it actually writes to the Event Store!
        commandService.cancelOrder(id);
        return ResponseEntity.ok("Order cancellation event appended to store for ID: " + id);
    }

    // NEW: The Event Replay Endpoint
    @GetMapping("/{id}/replay")
    public ResponseEntity<Order> replayOrderEvents(@PathVariable String id) {
        return ResponseEntity.ok(replayService.reconstructOrderState(id));
    }
}