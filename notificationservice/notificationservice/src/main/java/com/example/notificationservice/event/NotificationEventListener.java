package com.example.notificationservice.event;

import com.example.notificationservice.RabbitMQConfig;
import com.example.notificationservice.dto.EmployeeCreatedEventV1; // Import the Day 3 DTO
import com.example.notificationservice.entity.ProcessedEvent;
import com.example.notificationservice.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository; // Inbox repo
    private final RabbitTemplate rabbitTemplate;

    // ==========================================
    // 1. DAY 3: EMPLOYEE ONBOARDING WORKFLOW
    // ==========================================
    @RabbitListener(queues = RabbitMQConfig.EMPLOYEE_CREATED_QUEUE)
    public void handleEmployeeCreated(EmployeeCreatedEventV1 event) {
        try {
            // --- IDEMPOTENCY CHECK ---
            String uniqueEventId = "EMPLOYEE_CREATED_" + event.getEventId();
            if (processedEventRepository.existsById(uniqueEventId)) {
                log.warn("IDEMPOTENCY TRIGGERED: Already sent welcome email for {}. Dropping duplicate.", uniqueEventId);
                return;
            }

            log.info("EVENT RECEIVED [Notification Service]: EmployeeCreatedEventV1 for Employee ID: {}", event.getEmployeeId());
            log.info("Sending Welcome Email to: {}", event.getEmail());

            // --- RECORD SUCCESS ---
            processedEventRepository.save(new ProcessedEvent(uniqueEventId, LocalDateTime.now()));
            log.info("Event {} marked as processed.", uniqueEventId);

        } catch (Exception e) {
            log.error("Failed to process EmployeeCreatedEvent: {}", e.getMessage());
            throw new RuntimeException("Error processing employee creation, triggering retry/DLQ", e);
        }
    }

    // ==========================================
    // 2. LEAVE WORKFLOW (Your Existing Code)
    // ==========================================

    // Listen for Approved Events
    @RabbitListener(queues = RabbitMQConfig.APPROVED_QUEUE)
    public void handleLeaveApproved(String payload) {
        try {
            LeaveApprovedEvent event = objectMapper.readValue(payload, LeaveApprovedEvent.class);

            String uniqueEventId = "LEAVE_APPROVED_" + event.getLeaveId();
            if (processedEventRepository.existsById(uniqueEventId)) {
                log.warn("IDEMPOTENCY TRIGGERED: Already sent email for {}. Dropping duplicate.", uniqueEventId);
                return;
            }

            log.info("Received LeaveApprovedEvent!");
            log.info("Sending approval email to Employee ID: {}", event.getEmployeeId());

            processedEventRepository.save(new ProcessedEvent(uniqueEventId, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Failed to process LeaveApprovedEvent: {}", e.getMessage());
        }
    }

    // Listen for Applied Events
    @RabbitListener(queues = RabbitMQConfig.APPLIED_QUEUE)
    public void handleLeaveApplied(String payload) throws Exception {
        LeaveAppliedEvent event = objectMapper.readValue(payload, LeaveAppliedEvent.class);

        String uniqueEventId = "LEAVE_APPLIED_" + event.getLeaveId();
        if (processedEventRepository.existsById(uniqueEventId)) {
            log.warn("IDEMPOTENCY TRIGGERED: Already sent email for {}. Dropping duplicate message.", uniqueEventId);
            return;
        }

        log.info("Received LeaveAppliedEvent for Leave ID: {}", event.getLeaveId());

        try {
            // ACTUAL BUSINESS LOGIC
            log.info("Successfully sent application confirmation email to: {}", event.getEmployeeEmail());

            // RECORD SUCCESS
            processedEventRepository.save(new ProcessedEvent(uniqueEventId, LocalDateTime.now()));
            log.info("Event {} marked as processed.", uniqueEventId);

        } catch (Exception e) {
            // REAL SAGA COMPENSATION
            log.error("Real email service failure! Firing Saga Compensation Event back to Leave Service.", e);

            LeaveCompensationEvent compEvent = new LeaveCompensationEvent(
                    event.getLeaveId(),
                    event.getEmployeeId(),
                    "Email Delivery Failure: " + e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.COMPENSATION_ROUTING_KEY,
                    objectMapper.writeValueAsString(compEvent)
            );

            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Email Server error. Message sent to DLQ and Compensation fired.");
        }
    }

    // Listen for Rejected Events
    @RabbitListener(queues = RabbitMQConfig.REJECTED_QUEUE)
    public void handleLeaveRejected(String payload) {
        try {
            LeaveRejectedEvent event = objectMapper.readValue(payload, LeaveRejectedEvent.class);

            String uniqueEventId = "LEAVE_REJECTED_" + event.getLeaveId();
            if (processedEventRepository.existsById(uniqueEventId)) {
                log.warn("IDEMPOTENCY TRIGGERED: Already sent email for {}. Dropping duplicate.", uniqueEventId);
                return;
            }

            log.info("Received LeaveRejectedEvent!");
            log.info("Sending rejection email to Employee Email: {}", event.getEmployeeEmail());
            log.info("Rejection Reason: {}", event.getReason());

            processedEventRepository.save(new ProcessedEvent(uniqueEventId, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Failed to process LeaveRejectedEvent: {}", e.getMessage());
        }
    }
}