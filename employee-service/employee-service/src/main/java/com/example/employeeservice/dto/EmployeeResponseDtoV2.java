package com.example.employeeservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeResponseDtoV2 {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;
}