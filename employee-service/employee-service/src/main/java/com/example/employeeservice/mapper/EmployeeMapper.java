package com.example.employeeservice.mapper;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeRequestDto;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.example.employeeservice.infrastructure.entity.EmployeeEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    // --- 1. Database Entity <-> Domain Model Mappings ---

    public Employee toDomain(EmployeeEntity entity) {
        if (entity == null) return null;
        return Employee.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .departmentId(entity.getDepartmentId())
                .version(entity.getVersion()) // ---> ADDED: Map version from DB
                .build();
    }

    public EmployeeEntity toEntity(Employee domain) {
        if (domain == null) return null;
        return EmployeeEntity.builder()
                .id(domain.getId())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .departmentId(domain.getDepartmentId())
                .version(domain.getVersion()) // ---> ADDED: Map version to DB
                .build();
    }

    // --- 2. Domain Model <-> DTO Mappings ---

    public EmployeeResponseDto toDto(Employee employee) {
        if (employee == null) return null;
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .departmentId(employee.getDepartmentId())
                .version(employee.getVersion()) // ---> ADDED: Send version to client
                .build();
    }

    public Employee toDomain(EmployeeRequestDto dto) {
        if (dto == null) return null;
        return Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .departmentId(dto.getDepartmentId())
                .version(dto.getVersion()) // ---> ADDED: Receive version from client for safe updates
                .build();
    }
}