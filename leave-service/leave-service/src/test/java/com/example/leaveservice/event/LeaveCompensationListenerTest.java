package com.example.leaveservice.event;

import com.example.leaveservice.domain.Leave; // 👉 CHANGED: Using Domain model instead of Entity
import com.example.leaveservice.repository.LeaveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCompensationListenerTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LeaveCompensationListener listener;

    @Test
    void givenValidPayload_whenHandleCompensation_thenUpdateLeaveStatus() throws Exception {
        // Arrange
        String payload = "{\"leaveId\": 10, \"failureReason\": \"Employee Not Found\"}";
        LeaveCompensationEvent event = new LeaveCompensationEvent();
        event.setLeaveId(10L);
        event.setFailureReason("Employee Not Found");

        // 👉 CHANGED: Using the 'Leave' domain object
        Leave existingLeave = new Leave();
        existingLeave.setId(10L);
        existingLeave.setStatus("APPROVED");

        when(objectMapper.readValue(payload, LeaveCompensationEvent.class)).thenReturn(event);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(existingLeave));

        // Act
        listener.handleCompensation(payload);

        // Assert
        assertThat(existingLeave.getStatus()).isEqualTo("FAILED_COMPENSATED");
        verify(leaveRepository, times(1)).save(existingLeave);
    }

    @Test
    void givenDuplicateEvent_whenHandleCompensation_thenProcessIdempotently() throws Exception {
        // Arrange
        String payload = "{\"leaveId\": 10, \"failureReason\": \"Employee Not Found\"}";
        LeaveCompensationEvent event = new LeaveCompensationEvent();
        event.setLeaveId(10L);

        // 👉 CHANGED: Using the 'Leave' domain object
        Leave alreadyCompensatedLeave = new Leave();
        alreadyCompensatedLeave.setId(10L);
        alreadyCompensatedLeave.setStatus("FAILED_COMPENSATED");

        when(objectMapper.readValue(payload, LeaveCompensationEvent.class)).thenReturn(event);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(alreadyCompensatedLeave));

        // Act
        listener.handleCompensation(payload);

        // Assert
        assertThat(alreadyCompensatedLeave.getStatus()).isEqualTo("FAILED_COMPENSATED");
        verify(leaveRepository, times(1)).save(alreadyCompensatedLeave);
    }

    @Test
    void givenInvalidJsonPayload_whenHandleCompensation_thenThrowAmqpRejectToTriggerDLQ() throws Exception {
        // Arrange
        String badPayload = "INVALID_JSON_DATA";

        when(objectMapper.readValue(badPayload, LeaveCompensationEvent.class))
                .thenThrow(new RuntimeException("JSON Parse Error"));

        // Act & Assert: Fixed the lambda expression here!
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.handleCompensation(badPayload));

        // Verify
        verify(leaveRepository, never()).findById(any());
        verify(leaveRepository, never()).save(any());
    }
}