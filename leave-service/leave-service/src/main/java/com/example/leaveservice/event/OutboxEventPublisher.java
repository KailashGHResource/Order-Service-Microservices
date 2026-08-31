package com.example.leaveservice.event;

import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import com.example.leaveservice.repository.OutboxEventRepository;
import com.example.leaveservice.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 10000)
    public void publishEvents() {
        // FIXED: Changed OutboxEvent to OutboxEventEntity
        List<OutboxEventEntity> unprocessedEvents = outboxRepository.findByStatusIn(
                Arrays.asList(OutboxEventEntity.EventStatus.PENDING, OutboxEventEntity.EventStatus.FAILED)
        );

        // FIXED: Changed OutboxEvent to OutboxEventEntity
        for (OutboxEventEntity event : unprocessedEvents) {
            try {
                // 1. Determine the correct routing key based on the event type
                String routingKey = determineRoutingKey(event.getEventType());

                // 2. Publish to your existing exchange using the specific routing key
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        routingKey,
                        event.getPayload()
                );

                // 3. Mark successful events as PROCESSED
                event.setStatus(OutboxEventEntity.EventStatus.PROCESSED);
                log.info("Successfully published outbox event ID: {} to routing key: {}", event.getId(), routingKey);

            } catch (Exception e) {
                // 4. Increment retry count on failure
                event.setStatus(OutboxEventEntity.EventStatus.FAILED);
                event.setRetryCount(event.getRetryCount() + 1);
                log.error("Failed to publish outbox event ID: {}. Retry count: {}. Error: {}",
                        event.getId(), event.getRetryCount(), e.getMessage());
            }

            // Save the updated status and retry count back to the database
            outboxRepository.save(event);
        }
    }

    // Helper method to map your Outbox event types to your specific RabbitMQ routing keys
    private String determineRoutingKey(String eventType) {
        switch (eventType) {
            case "LEAVE_APPLIED":
                return RabbitMQConfig.APPLIED_ROUTING_KEY;
            case "LEAVE_APPROVED":
                return RabbitMQConfig.APPROVED_ROUTING_KEY;
            case "LEAVE_REJECTED":
                return RabbitMQConfig.REJECTED_ROUTING_KEY;
            default:
                log.warn("Unknown event type: {}. Sending to default routing key.", eventType);
                return "leave.default.key";
        }
    }
}