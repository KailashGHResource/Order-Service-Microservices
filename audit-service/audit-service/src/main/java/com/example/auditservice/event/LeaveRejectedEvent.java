package com.example.auditservice.event;
import lombok.Data;

@Data
public class LeaveRejectedEvent {
    private Long leaveId;
    private Long employeeId;
    private String employeeEmail;
    private String status;
    private String reason;
}