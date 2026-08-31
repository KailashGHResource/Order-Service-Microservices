package com.example.leaveservice.repository;

import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.LeaveFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LeaveRepository {
    Leave save(Leave leave);
    Optional<Leave> findById(Long id);
    List<Leave> findAll();

    // Advanced Search Port
    Page<Leave> findWithFilters(LeaveFilterDto filter, Pageable pageable);
    List<Leave> findByEmployeeId(Long employeeId);

    // Bulk Operations support
    List<Leave> saveAll(List<Leave> leaves);
    List<Leave> findAllById(List<Long> ids);
}