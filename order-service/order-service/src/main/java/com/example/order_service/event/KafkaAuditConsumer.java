package com.example.order_service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaAuditConsumer {

    @KafkaListener(topics = "order-events", groupId = "audit-group")
    public void consumeAudit(OrderCreatedEvent event) {
        log.info("🛡️ [KAFKA AUDIT] - Logging compliance event to data lake. Order ID: {}",
                event.getOrderId());
    }
}