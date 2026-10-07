package com.example.order_service.event;

import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderItem;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProjector {

    // The Projector ONLY talks to the Read Database
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "order-events", groupId = "order-read-model-updater-group")
    @Transactional
    public void projectOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("📥 [READ MODEL UPDATER] Received OrderCreatedEvent for Order ID: {}", event.getOrderId());

        // 1. Idempotency Check (Duplicate Event Handling - Story 6 Requirement)
        if (orderRepository.existsById(event.getOrderId())) {
            log.warn("⚠️ Order ID {} already exists in Read DB. Ignoring duplicate event.", event.getOrderId());
            return;
        }

        // 2. Map the Event data to the Read Database Entity
        Order readModelOrder = new Order();
        readModelOrder.setId(event.getOrderId());
        readModelOrder.setCustomerId(event.getCustomerId());
        readModelOrder.setStatus(OrderStatus.CREATED);
        readModelOrder.setTotalAmount(event.getTotalAmount());

        // FIX: Initialize the version for Optimistic Locking to prevent the Hibernate exception!
        readModelOrder.setVersion(0L);

        // Map items
        if (event.getItems() != null) {
            for (var itemEvent : event.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(readModelOrder);
                item.setProductId(itemEvent.getProductId());
                item.setQuantity(itemEvent.getQuantity());
                item.setPrice(itemEvent.getPrice());
                readModelOrder.getItems().add(item);
            }
        }

        // 3. Save to the Read Database
        orderRepository.save(readModelOrder);
        log.info("✅ [READ MODEL UPDATER] Successfully synchronized Order ID {} to the Read Database", event.getOrderId());
    }
}