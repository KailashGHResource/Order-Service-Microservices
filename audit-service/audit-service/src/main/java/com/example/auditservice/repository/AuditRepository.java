package com.example.auditservice.repository;

import com.example.auditservice.domain.AuditLog;
import java.util.List;

public interface AuditRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findAll();
}