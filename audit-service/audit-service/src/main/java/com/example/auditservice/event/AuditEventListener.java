package com.example.auditservice.event;

import com.example.auditservice.application.AuditService;
import com.example.auditservice.dto.AuditEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    // ==========================================
    // 1. EMPLOYEE ONBOARDING AUDIT
    // ==========================================
    @RabbitListener(queues = "audit.employee.created.queue")
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {
        try {
            log.info("EVENT RECEIVED [Audit Service]: Logging creation of Employee ID: {}", event.getEmployeeId());

            AuditEventDto auditLog = new AuditEventDto();
            auditLog.setAction("EMPLOYEE_CREATED");
            auditLog.setPerformedBy("SYSTEM");
            auditLog.setEntityId(String.valueOf(event.getEmployeeId()));
            auditLog.setDetails("Employee " + event.getFirstName() + " " + event.getLastName() + " onboarded successfully.");
            auditLog.setTimestamp(LocalDateTime.now());

            auditService.logActivity(auditLog);
            log.info("Successfully audited event for Employee ID: {}", event.getEmployeeId());

        } catch (Exception e) {
            log.error("Failed to process EmployeeCreatedEvent for Audit: {}", e.getMessage());
            throw new RuntimeException("Audit database error, triggering retry/DLQ", e);
        }
    }

    // ==========================================
    // 2. LEAVE APPLIED AUDIT
    // ==========================================
    // Keep your existing handleEmployeeCreated method above...

    // ==========================================
    // 2. LEAVE APPLIED AUDIT
    // ==========================================
    @RabbitListener(queues = "audit.leave.applied.queue")
    public void handleLeaveApplied(String payload) { // CHANGE: Accept String payload
        try {
            // CHANGE: Manually deserialize the string into the object
            ObjectMapper objectMapper = new ObjectMapper();
            // You may need to register the JavaTimeModule if you use LocalDate
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            LeaveAppliedEvent event = objectMapper.readValue(payload, LeaveAppliedEvent.class);

            log.info("EVENT RECEIVED [Audit Service]: Logging Leave Applied for Leave ID: {}", event.getLeaveId());

            AuditEventDto auditLog = new AuditEventDto();
            auditLog.setAction("LEAVE_APPLIED");
            auditLog.setPerformedBy("SYSTEM");
            auditLog.setEntityId(String.valueOf(event.getLeaveId()));
            auditLog.setDetails("Leave applied from " + event.getStartDate() + " to " + event.getEndDate() + " for Employee ID " + event.getEmployeeId());
            auditLog.setTimestamp(LocalDateTime.now());

            auditService.logActivity(auditLog);
            log.info("Successfully audited Leave event for Leave ID: {}", event.getLeaveId());

        } catch (Exception e) {
            log.error("Failed to process LeaveAppliedEvent for Audit: {}", e.getMessage());
            throw new RuntimeException("Audit database error, triggering retry/DLQ", e);
        }

    }
}