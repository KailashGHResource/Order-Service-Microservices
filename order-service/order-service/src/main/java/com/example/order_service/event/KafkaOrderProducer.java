package com.example.order_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaOrderProducer {

    // KafkaTemplate is Spring's core tool for sending messages
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderEvent(OrderCreatedEvent event) {
        log.info("🚀 [KAFKA PRODUCER] - Sending OrderCreatedEvent for Order ID: {}", event.getOrderId());

        // We use MessageBuilder to cleanly attach the Topic and a Message Key.
        // Using the OrderID as the key ensures all updates for this specific order go to the same partition!
        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "order-events")
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getOrderId()))
                .build();

        kafkaTemplate.send(message);
    }

    public void sendRefundPaymentEvent(RefundPaymentEvent event) {
        log.info("💸 [KAFKA PRODUCER] - Sending RefundPaymentEvent for Order ID: {}", event.getOrderId());
        Message<RefundPaymentEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "payment-events")
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getOrderId()))
                .build();
        kafkaTemplate.send(message);
    }

    public void sendReleaseInventoryEvent(ReleaseInventoryEvent event) {
        log.info("📦 [KAFKA PRODUCER] - Sending ReleaseInventoryEvent for Order ID: {}", event.getOrderId());
        Message<ReleaseInventoryEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "inventory-events")
                .setHeader(KafkaHeaders.KEY, String.valueOf(event.getOrderId()))
                .build();
        kafkaTemplate.send(message);
    }
}