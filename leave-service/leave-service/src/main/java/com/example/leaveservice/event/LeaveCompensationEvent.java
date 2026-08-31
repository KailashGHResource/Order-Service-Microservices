package com.example.leaveservice.event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCompensationEvent {
    private Long leaveId;
    private Long employeeId;
    private String failureReason;
}