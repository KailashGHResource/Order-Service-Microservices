package com.example.departmentservice.application;

import com.example.departmentservice.domain.Department;
import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.dto.DepartmentResponseDto;
import com.example.departmentservice.mapper.DepartmentMapper;
import com.example.departmentservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto) {
        log.info("Creating new department with name: {}", requestDto.getName());
        Department department = departmentMapper.toDomain(requestDto);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDto(savedDepartment);
    }

    public List<DepartmentResponseDto> getAllDepartments() {
        log.info("Fetching all departments from database");
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    public DepartmentResponseDto getDepartmentById(Long id) {
        log.info("Fetching department details for ID: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", id);
                    return new RuntimeException("Department not found with ID: " + id);
                });
        return departmentMapper.toDto(department);
    }

    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto updatedDetails) {
        log.info("Updating department details for ID: {}", id);
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update. Department not found with ID: {}", id);
                    return new RuntimeException("Department not found with ID: " + id);
                });

        // Update fields
        existingDepartment.setName(updatedDetails.getName());
        existingDepartment.setDescription(updatedDetails.getDescription());
        if (updatedDetails.getCode() != null) {
            existingDepartment.setCode(updatedDetails.getCode());
        }

        Department savedDepartment = departmentRepository.save(existingDepartment);
        log.info("Successfully updated department details for ID: {}", id);
        return departmentMapper.toDto(savedDepartment);
    }

    public void deleteDepartment(Long id) {
        log.info("Request received to delete Department ID: {}", id);
        // We use findById to ensure it exists before deleting (matches your old logic)
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot delete. Department not found with ID: {}", id);
                    return new RuntimeException("Department not found with ID: " + id);
                });

        departmentRepository.deleteById(existingDepartment.getId());
        log.info("Department with ID {} deleted successfully", id);
    }

    public void validateDepartmentExists(Long departmentId) {
        // Business logic check used by the assignment endpoint
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.error("Cannot assign. Department not found with ID: {}", departmentId);
                    return new RuntimeException("Department not found with ID: " + departmentId);
                });
    }
}