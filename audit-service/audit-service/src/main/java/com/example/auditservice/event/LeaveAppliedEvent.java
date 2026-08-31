package com.example.auditservice.event;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveAppliedEvent {
    private Long leaveId;
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String employeeEmail;
}