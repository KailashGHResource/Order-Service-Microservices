package com.example.leaveservice.event;

import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import com.example.leaveservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void givenPendingEvent_whenPublish_thenSendToRabbitAndMarkProcessed() {
        // Arrange: Create a PENDING event ready to be published
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setEventType("LEAVE_APPLIED");
        event.setPayload("{\"leaveId\": 10}");
        event.setStatus(OutboxEventEntity.EventStatus.PENDING);
        event.setRetryCount(0);

        // Tell Mockito to return this event when the scheduled job checks the DB
        when(outboxRepository.findByStatusIn(anyList())).thenReturn(List.of(event));

        // Act: Manually trigger the scheduled method
        outboxEventPublisher.publishEvents();

        // Assert 1: Verify it was sent to RabbitMQ (using anyString() for exchange to avoid needing RabbitMQConfig directly)
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), eq("{\"leaveId\": 10}"));

        // Assert 2: Verify the status was updated to PROCESSED
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.EventStatus.PROCESSED);

        // Assert 3: Verify the changes were saved to the DB
        verify(outboxRepository, times(1)).save(event);
    }

    @Test
    void givenRabbitMqError_whenPublish_thenMarkFailedAndIncrementRetry() {
        // Arrange: Create a PENDING event
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(2L);
        event.setEventType("LEAVE_APPROVED");
        event.setPayload("{\"leaveId\": 20}");
        event.setStatus(OutboxEventEntity.EventStatus.PENDING);
        event.setRetryCount(0); // Starts at 0

        when(outboxRepository.findByStatusIn(anyList())).thenReturn(List.of(event));

        // Simulate RabbitMQ being down / throwing an exception when we try to send
        doThrow(new AmqpException("RabbitMQ Connection Refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act
        outboxEventPublisher.publishEvents();

        // Assert 1: Event status should be safely degraded to FAILED
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.EventStatus.FAILED);

        // Assert 2: Retry count must have incremented!
        assertThat(event.getRetryCount()).isEqualTo(1);

        // Assert 3: Ensure we saved the failure state to the DB so it can be retried next time
        verify(outboxRepository, times(1)).save(event);
    }

    @Test
    void givenUnknownEventType_whenPublish_thenRouteToDefaultKey() {
        // Arrange: Create an event with an unknown type that should trigger your `default:` switch case
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(3L);
        event.setEventType("UNKNOWN_MYSTERY_EVENT");
        event.setPayload("{\"data\": \"unknown\"}");
        event.setStatus(OutboxEventEntity.EventStatus.FAILED); // Test picking up a previously failed event
        event.setRetryCount(1);

        when(outboxRepository.findByStatusIn(anyList())).thenReturn(List.of(event));

        // Act
        outboxEventPublisher.publishEvents();

        // Assert: Verify it routed to "leave.default.key" as defined in your fallback switch case
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("leave.default.key"), eq("{\"data\": \"unknown\"}"));
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.EventStatus.PROCESSED);
    }
}