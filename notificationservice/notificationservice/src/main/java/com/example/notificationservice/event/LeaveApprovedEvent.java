package com.example.notificationservice.event;
import lombok.Data;

@Data
public class LeaveApprovedEvent {

    private Long leaveId;
    private Long employeeId;
    private String status;
    private String employeeEmail;

}