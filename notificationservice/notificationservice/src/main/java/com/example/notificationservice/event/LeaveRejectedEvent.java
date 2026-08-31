package com.example.notificationservice.event; //

import lombok.Data;

@Data
public class LeaveRejectedEvent {
    private Long leaveId;
    private Long employeeId;
    private String status;
    private String reason;
    private String employeeEmail;
}