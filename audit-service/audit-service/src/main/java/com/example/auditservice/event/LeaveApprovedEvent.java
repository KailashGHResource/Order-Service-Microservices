package com.example.auditservice.event;

import lombok.Data;

@Data
public class LeaveApprovedEvent {
    private Long leaveId;
    private Long employeeId;
    private String employeeEmail;
}