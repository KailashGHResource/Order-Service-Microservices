package com.example.auditservice.infrastructure.repository;

import com.example.auditservice.infrastructure.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// The <AuditLogEntity, Long> part is critical! It tells Spring exactly what object to return.
public interface AuditJpaRepository extends JpaRepository<AuditLogEntity, Long> {
}