package com.example.leaveservice.infrastructure.repository;

import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// Add JpaSpecificationExecutor<LeaveEntity> here
public interface LeaveJpaRepository extends JpaRepository<LeaveEntity, Long>, JpaSpecificationExecutor<LeaveEntity> {

    List<LeaveEntity> findByEmployeeId(Long employeeId);
}