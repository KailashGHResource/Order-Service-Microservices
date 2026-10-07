package com.example.order_service.application;

import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaRecoverySweeper {

    private final OrderRepository orderRepository; // Reading from the Query side
    private final OrderCommandService orderCommandService; // Writing to the Command side

    // Runs every 15 minutes (900,000 ms)
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void recoverStuckOrders() {
        log.info("Starting Saga Recovery Sweeper to check for stuck orders...");

        // Define our threshold: any order stuck for more than 30 minutes
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        // 1. Recover stuck PAYMENT_PENDING orders
        List<Order> stuckPaymentOrders = orderRepository.findStuckOrders(OrderStatus.PAYMENT_PENDING, threshold);
        for (Order order : stuckPaymentOrders) {
            log.warn("SAGA RECOVERY: Order ID {} has been stuck in PAYMENT_PENDING for too long. Cancelling...", order.getId());
            try {
                // Issues a command to append an OrderCancelledEvent
                orderCommandService.cancelOrder(order.getId());
            } catch (Exception e) {
                log.error("Failed to recover stuck order {}: {}", order.getId(), e.getMessage());
            }
        }

        // 2. Recover stuck CONFIRMED orders (Inventory reserved, but payment never even initiated)
        List<Order> stuckConfirmedOrders = orderRepository.findStuckOrders(OrderStatus.CONFIRMED, threshold);
        for (Order order : stuckConfirmedOrders) {
            log.warn("SAGA RECOVERY: Order ID {} has been stuck in CONFIRMED for too long. Cancelling...", order.getId());
            try {
                // Issues a command to append an OrderCancelledEvent
                orderCommandService.cancelOrder(order.getId());
            } catch (Exception e) {
                log.error("Failed to recover stuck order {}: {}", order.getId(), e.getMessage());
            }
        }

        log.info("Saga Recovery Sweeper finished.");
    }
}