package com.example.order_service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaNotificationConsumer {

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeNotification(OrderCreatedEvent event) {
        log.info("🔔 [KAFKA NOTIFICATION] - Emailing customer {} for Order ID: {}",
                event.getCustomerId(), event.getOrderId());
    }
}