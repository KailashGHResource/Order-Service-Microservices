package com.example.leaveservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveFilterDto {
    private Long employeeId;
    private String leaveType; // e.g., SICK, CASUAL, ANNUAL
    private String status;    // e.g., PENDING, APPROVED, REJECTED
    private LocalDate startDate;
    private LocalDate endDate;
}