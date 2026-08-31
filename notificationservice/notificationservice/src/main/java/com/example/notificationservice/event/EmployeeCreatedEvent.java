package com.example.notificationservice.event; // Update to com.example.employeeservice.event for the Employee Service

import lombok.Data;

@Data
public class EmployeeCreatedEvent {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String employeeEmail;
    private String department;
}