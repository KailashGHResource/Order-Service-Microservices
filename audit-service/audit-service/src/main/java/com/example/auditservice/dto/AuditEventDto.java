package com.example.auditservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditEventDto {
    private String action;
    private String performedBy;
    private String entityId;
    private String details;
    private LocalDateTime timestamp;
}