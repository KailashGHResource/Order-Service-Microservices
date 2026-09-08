package com.example.leaveservice.event;

import com.example.leaveservice.RabbitMQConfig;
import com.example.leaveservice.repository.LeaveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException; // ADD THIS
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveCompensationListener {

    private final LeaveRepository leaveRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.COMPENSATION_QUEUE)
    @Transactional
    public void handleCompensation(String payload) {
        try {
            LeaveCompensationEvent event = objectMapper.readValue(payload, LeaveCompensationEvent.class);
            log.warn("🚨 SAGA COMPENSATION RECEIVED for Leave ID: {}. Reason: {}", event.getLeaveId(), event.getFailureReason());

            leaveRepository.findById(event.getLeaveId()).ifPresentOrElse(leave -> {
                leave.setStatus("FAILED_COMPENSATED");
                leaveRepository.save(leave);
                log.info("Leave ID: {} status successfully rolled back to FAILED_COMPENSATED", leave.getId());
            }, () -> log.error("Leave ID: {} not found for compensation!", event.getLeaveId()));

        } catch (Exception e) {
            log.error("Failed to process compensation event: {}", e.getMessage(), e);
            // 👉 NEW: This forces RabbitMQ to route the message to the DLQ!
            throw new AmqpRejectAndDontRequeueException("Routing message to DLQ due to error", e);
        }
    }
}