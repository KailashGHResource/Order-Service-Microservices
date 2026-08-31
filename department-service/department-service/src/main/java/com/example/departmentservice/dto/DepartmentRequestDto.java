package com.example.departmentservice.dto;
import lombok.Data;

@Data
public class DepartmentRequestDto {
    private String name;
    private String code;
    private String description;
}