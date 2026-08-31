package com.example.auditservice.mapper;

import com.example.auditservice.domain.AuditLog;
import com.example.auditservice.dto.AuditEventDto;
import com.example.auditservice.dto.AuditResponseDto;
import com.example.auditservice.infrastructure.entity.AuditLogEntity;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class AuditMapper {

    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) return null;
        return AuditLog.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .performedBy(entity.getPerformedBy())
                .entityId(entity.getEntityId())
                .details(entity.getDetails())
                .timestamp(entity.getTimestamp())
                .build();
    }

    public AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) return null;
        return AuditLogEntity.builder()
                .id(domain.getId())
                .action(domain.getAction())
                .performedBy(domain.getPerformedBy())
                .entityId(domain.getEntityId())
                .details(domain.getDetails())
                .timestamp(domain.getTimestamp())
                .build();
    }

    public AuditLog toDomain(AuditEventDto dto) {
        if (dto == null) return null;
        return AuditLog.builder()
                .action(dto.getAction())
                .performedBy(dto.getPerformedBy())
                .entityId(dto.getEntityId())
                .details(dto.getDetails())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .build();
    }

    public AuditResponseDto toDto(AuditLog domain) {
        if (domain == null) return null;
        return AuditResponseDto.builder()
                .id(domain.getId())
                .action(domain.getAction())
                .performedBy(domain.getPerformedBy())
                .entityId(domain.getEntityId())
                .details(domain.getDetails())
                .timestamp(domain.getTimestamp())
                .build();
    }
}