package com.example.employeeservice.dto;

import lombok.Data;

@Data
public class EmployeeFilterDto {
    private String name;
    private String email;
    private Long departmentId;
}