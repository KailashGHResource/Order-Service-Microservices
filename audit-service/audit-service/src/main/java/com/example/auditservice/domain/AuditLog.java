package com.example.auditservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private String action;       // e.g., "EMPLOYEE_CREATED"
    private String performedBy;  // e.g., "SYSTEM" or "USER_101"
    private String entityId;     // The ID of the affected record
    private String details;
    private LocalDateTime timestamp;
    private String eventType;
    private String payload;
}
