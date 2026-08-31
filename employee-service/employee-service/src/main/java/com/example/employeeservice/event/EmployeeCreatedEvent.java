package com.example.employeeservice.event;

import lombok.Data;

@Data
public class EmployeeCreatedEvent {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String employeeEmail;
    private String department;
}