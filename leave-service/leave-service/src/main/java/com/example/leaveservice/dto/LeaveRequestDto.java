package com.example.leaveservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDto {
    private Long employeeId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
}