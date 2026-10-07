package com.example.order_service.controller;

import com.example.order_service.event.SagaEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mock-saga")
@RequiredArgsConstructor
public class MockSagaController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/inventory")
    public String mockInventoryEvent(@RequestParam Long orderId, @RequestParam String status) {
        SagaEvents.InventoryReservedEvent event = SagaEvents.InventoryReservedEvent.builder()
                .orderId(orderId)
                .status(status) // "SUCCESS" or "FAILED"
                .reason(status.equals("SUCCESS") ? "Items reserved" : "Out of stock")
                .build();

        Message<SagaEvents.InventoryReservedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "inventory-events")
                .setHeader(KafkaHeaders.KEY, String.valueOf(orderId))
                .build();

        kafkaTemplate.send(message);
        return "Mock Inventory Event (" + status + ") sent to Kafka for Order " + orderId;
    }

    @PostMapping("/payment")
    public String mockPaymentEvent(@RequestParam Long orderId, @RequestParam String status) {
        SagaEvents.PaymentProcessedEvent event = SagaEvents.PaymentProcessedEvent.builder()
                .orderId(orderId)
                .status(status) // "SUCCESS" or "FAILED"
                .reason(status.equals("SUCCESS") ? "Payment processed" : "Card declined")
                .build();

        Message<SagaEvents.PaymentProcessedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "payment-events")
                .setHeader(KafkaHeaders.KEY, String.valueOf(orderId))
                .build();

        kafkaTemplate.send(message);
        return "Mock Payment Event (" + status + ") sent to Kafka for Order " + orderId;
    }
}