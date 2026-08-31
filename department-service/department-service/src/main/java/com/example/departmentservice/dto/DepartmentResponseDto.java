package com.example.departmentservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentResponseDto {
    private Long id;
    private String name;
    private String code;
    private String description;
}