package com.example.order_service.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockSagaEventSimulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Map<Long, Long> orderCustomerMap = new ConcurrentHashMap<>();
    // 🔥 NEW: Tracks the exact state of the mock to prevent infinite loops
    private final Map<Long, String> orderMockState = new ConcurrentHashMap<>();

    private JsonNode extractJson(Object value) throws Exception {
        if (value instanceof String) return objectMapper.readTree((String) value);
        return objectMapper.readTree(objectMapper.writeValueAsString(value));
    }

    @KafkaListener(topics = "order-events", groupId = "mock-inventory-group")
    public void simulateInventoryReservation(ConsumerRecord<String, Object> record) {
        try {
            JsonNode json = extractJson(record.value());

            if (json.hasNonNull("customerId") && json.hasNonNull("items")) {
                Long orderId = json.get("orderId").asLong();
                Long customerId = json.get("customerId").asLong();

                orderCustomerMap.put(orderId, customerId);
                orderMockState.put(orderId, "INVENTORY_CHECK");

                log.info("👽 [MOCK INVENTORY] - Checking stock for Order ID: {} (Customer: {})", orderId, customerId);
                Thread.sleep(1500);

                if (customerId == 2L) {
                    orderMockState.put(orderId, "FAILED");
                    log.warn("👽 [MOCK INVENTORY] - OUT OF STOCK for Order ID: {}", orderId);
                    kafkaTemplate.send("inventory-events", String.format("{\"orderId\": %d, \"status\": \"FAILED\", \"reason\": \"Out of Stock\"}", orderId));
                    return;
                }

                // Mark inventory as successful so the payment mock knows it is safe to proceed
                orderMockState.put(orderId, "INVENTORY_SUCCESS");
                kafkaTemplate.send("inventory-events", String.format("{\"orderId\": %d, \"status\": \"SUCCESS\", \"reason\": \"Stock Reserved\"}", orderId));
            }
        } catch (Exception e) {
            log.error("Mock Inventory Failed", e);
        }
    }

    @KafkaListener(topics = "order-events", groupId = "mock-payment-group")
    public void simulatePaymentProcessing(ConsumerRecord<String, Object> record) {
        try {
            JsonNode json = extractJson(record.value());

            if (json.hasNonNull("orderId")) {
                Long orderId = json.get("orderId").asLong();

                // 🔥 FIX: Only process payment if the mock specifically knows inventory just succeeded.
                // This completely ignores the stripped cancellation events looping in the background.
                if (!"INVENTORY_SUCCESS".equals(orderMockState.get(orderId))) {
                    return;
                }

                // Immediately transition state so it cannot run twice
                orderMockState.put(orderId, "PAYMENT_PROCESSING");
                Long customerId = orderCustomerMap.getOrDefault(orderId, 1L);

                log.info("👽 [MOCK PAYMENT] - Processing payment for Order ID: {} (Customer: {})", orderId, customerId);
                Thread.sleep(1500);

                if (customerId == 3L) {
                    orderMockState.put(orderId, "FAILED");
                    log.warn("👽 [MOCK PAYMENT] - CARD DECLINED for Order ID: {}", orderId);
                    kafkaTemplate.send("payment-events", String.format("{\"orderId\": %d, \"status\": \"FAILED\", \"reason\": \"Card Declined\"}", orderId));
                    return;
                }

                orderMockState.put(orderId, "DONE");
                kafkaTemplate.send("payment-events", String.format("{\"orderId\": %d, \"status\": \"SUCCESS\", \"reason\": \"Payment Processed\"}", orderId));
            }
        } catch (Exception e) {
            log.error("Mock Payment Failed", e);
        }
    }
}