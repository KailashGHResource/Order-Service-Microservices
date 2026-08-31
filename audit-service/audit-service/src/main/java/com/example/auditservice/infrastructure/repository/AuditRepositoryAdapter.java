package com.example.auditservice.infrastructure.repository;

import com.example.auditservice.domain.AuditLog;
import com.example.auditservice.infrastructure.entity.AuditLogEntity; // Make sure this is imported!
import com.example.auditservice.mapper.AuditMapper;
import com.example.auditservice.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepository {

    private final AuditJpaRepository jpaRepository;
    private final AuditMapper auditMapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        // Explicitly defining the type helps the IDE resolve the save() method
        AuditLogEntity entityToSave = auditMapper.toEntity(auditLog);

        AuditLogEntity savedEntity = jpaRepository.save(entityToSave);

        return auditMapper.toDomain(savedEntity);
    }

    @Override
    public List<AuditLog> findAll() {
        return jpaRepository.findAll().stream()
                // Using an explicit lambda expression fixes the ambiguous method error!
                .map(entity -> auditMapper.toDomain(entity))
                .collect(Collectors.toList());
    }
}