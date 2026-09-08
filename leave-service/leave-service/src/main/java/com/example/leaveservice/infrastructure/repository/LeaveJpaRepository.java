package com.example.leaveservice.infrastructure.repository;

import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveJpaRepository extends JpaRepository<LeaveEntity, Long>, JpaSpecificationExecutor<LeaveEntity> {

    // OPTIMIZATION 1: Read-Only Query Hint.
    // Disables Hibernate Dirty-Checking, significantly reducing Memory and CPU usage for read operations.
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<LeaveEntity> findByEmployeeId(Long employeeId);
}