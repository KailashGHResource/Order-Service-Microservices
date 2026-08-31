package com.example.employeeservice.infrastructure.repository;

import com.example.employeeservice.infrastructure.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// NEW: Added JpaSpecificationExecutor<EmployeeEntity> here!
public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, Long>, JpaSpecificationExecutor<EmployeeEntity> {
    Optional<EmployeeEntity> findFirstByEmail(String email);
}