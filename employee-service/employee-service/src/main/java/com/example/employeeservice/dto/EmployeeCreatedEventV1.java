package com.example.employeeservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreatedEventV1 implements Serializable {
    private String eventId;
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime timestamp;
}