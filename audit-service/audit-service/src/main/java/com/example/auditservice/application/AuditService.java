package com.example.auditservice.application;

import com.example.auditservice.domain.AuditLog;
import com.example.auditservice.dto.AuditEventDto;
import com.example.auditservice.dto.AuditResponseDto;
import com.example.auditservice.mapper.AuditMapper;
import com.example.auditservice.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;

    // ---> THIS WAS MISSING: The method to save new logs from RabbitMQ <---
    public void logActivity(AuditEventDto eventDto) {
        log.info("Processing audit event for entity ID: {}", eventDto.getEntityId());

        // Convert DTO to Domain, then save using the Port
        AuditLog domainLog = auditMapper.toDomain(eventDto);
        auditRepository.save(domainLog);

        log.info("Successfully saved audit log for action: {}", eventDto.getAction());
    }

    // The method used by the Controller
    public List<AuditResponseDto> getAllAuditLogs() {
        log.info("Fetching all audit logs from the database");
        return auditRepository.findAll().stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }
}