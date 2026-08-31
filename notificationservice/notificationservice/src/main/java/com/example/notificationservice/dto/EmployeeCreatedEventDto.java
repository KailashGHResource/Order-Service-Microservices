package com.example.notificationservice.dto;

import lombok.Data;

@Data
public class EmployeeCreatedEventDto {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String employeeEmail;
}