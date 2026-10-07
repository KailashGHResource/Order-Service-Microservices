package com.example.order_service.event;

import com.example.order_service.application.OrderCommandService;
import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSagaConsumer {

    private final OrderRepository orderRepository;
    private final OrderCommandService orderCommandService;
    private final ObjectMapper objectMapper;

    private JsonNode extractJson(Object value) throws Exception {
        if (value instanceof String) return objectMapper.readTree((String) value);
        return objectMapper.readTree(objectMapper.writeValueAsString(value));
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-saga-group")
    @Transactional
    public void handleInventoryEvent(ConsumerRecord<String, Object> record) {
        try {
            JsonNode event = extractJson(record.value());
            Long orderId = event.get("orderId").asLong();
            String status = event.get("status").asText();

            log.info("📦 [SAGA FORWARD] - Received Inventory Event for Order {}: {}", orderId, status);

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return;

            if ("SUCCESS".equals(status)) {
                if (!order.getStatus().canTransitionTo(OrderStatus.CONFIRMED)) return;
                orderCommandService.confirmOrder(orderId);
            } else {
                if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) return;
                log.error("❌ [SAGA COMPENSATION] - Inventory failed for Order {}. Cancelling order.", orderId);
                orderCommandService.cancelOrder(orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process inventory event", e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentEvent(ConsumerRecord<String, Object> record) {
        try {
            JsonNode event = extractJson(record.value());
            Long orderId = event.get("orderId").asLong();
            String status = event.get("status").asText();

            log.info("💸 [SAGA FORWARD] - Received Payment Event for Order {}: {}", orderId, status);

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return;

            if ("SUCCESS".equals(status)) {
                if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) return;
                orderCommandService.markOrderPaid(orderId);
                log.info("🎉 [SAGA COMPLETE] - Order {} is successfully PAID!", orderId);
            } else {
                if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) return;
                log.error("❌ [SAGA COMPENSATION] - Payment failed for Order {}. Triggering rollback.", orderId);
                orderCommandService.cancelOrder(orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event", e);
        }
    }
}