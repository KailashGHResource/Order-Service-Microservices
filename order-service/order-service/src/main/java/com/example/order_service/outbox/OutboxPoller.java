package com.example.order_service.outbox;

import com.example.order_service.event.KafkaOrderProducer;
import com.example.order_service.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaOrderProducer kafkaOrderProducer; // <-- Swapped to Kafka Producer
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Runs every 5 seconds
    @Transactional
    public void pollOutbox() {
        List<OutboxMessage> pendingMessages = outboxRepository.findByStatus("PENDING");

        for (OutboxMessage message : pendingMessages) {
            try {
                // Deserialize JSON payload back to event object using the class directly
                OrderCreatedEvent event = objectMapper.readValue(message.getPayload(), OrderCreatedEvent.class);

                // Publish to Kafka
                kafkaOrderProducer.sendOrderEvent(event);

                // Mark as processed
                message.setStatus("PROCESSED");
                outboxRepository.save(message);

                log.info("Outbox message ID {} successfully published to Kafka and marked PROCESSED.", message.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox message ID {}: {}", message.getId(), e.getMessage());
            }
        }
    }
}