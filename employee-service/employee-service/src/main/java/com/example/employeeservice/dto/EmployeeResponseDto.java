package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDto implements Serializable{
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;

    // ---> ADDED FOR OPTIMISTIC LOCKING <---
    private Long version;
}