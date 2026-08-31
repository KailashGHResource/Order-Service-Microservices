package com.example.leaveservice.event; // Update package name for Leave Service

import lombok.Data;

@Data
public class LeaveAppliedEvent {
    private Long leaveId;
    private Long employeeId;
    private String startDate;
    private String endDate;
    private String employeeEmail;
}