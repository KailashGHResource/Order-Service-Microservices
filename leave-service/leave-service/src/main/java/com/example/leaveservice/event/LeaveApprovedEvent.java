package com.example.leaveservice.event;
import lombok.Data;

@Data // This automatically generates your getters and setters!
public class LeaveApprovedEvent {
    private Long leaveId;
    private Long employeeId;
    private String status;
    private String employeeEmail;
}