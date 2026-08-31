package com.example.departmentservice.mapper;

import com.example.departmentservice.domain.Department;
import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.dto.DepartmentResponseDto;
import com.example.departmentservice.infrastructure.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    // --- 1. Database Entity <-> Domain Model Mappings ---

    public Department toDomain(DepartmentEntity entity) {
        if (entity == null) return null;
        return Department.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .build();
    }

    public DepartmentEntity toEntity(Department domain) {
        if (domain == null) return null;
        return DepartmentEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .code(domain.getCode())
                .description(domain.getDescription())
                .build();
    }

    // --- 2. Request/Response DTO <-> Domain Model Mappings ---

    public Department toDomain(DepartmentRequestDto dto) {
        if (dto == null) return null;
        return Department.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .build();
    }

    public DepartmentResponseDto toDto(Department domain) {
        if (domain == null) return null;
        return DepartmentResponseDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .code(domain.getCode())
                .description(domain.getDescription())
                .build();
    }
}