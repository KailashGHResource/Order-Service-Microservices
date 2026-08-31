package com.example.auditservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditResponseDto {
    private Long id;
    private String action;
    private String performedBy;
    private String entityId;
    private String details;
    private LocalDateTime timestamp;
}