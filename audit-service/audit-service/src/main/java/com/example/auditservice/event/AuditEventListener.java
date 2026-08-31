package com.example.auditservice.event;

import com.example.auditservice.application.AuditService;
import com.example.auditservice.dto.AuditEventDto;
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

    @RabbitListener(queues = "audit.employee.created.queue")
    public void handleEmployeeCreated(EmployeeCreatedEvent event) { // <--- Use EmployeeCreatedEvent here
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
}