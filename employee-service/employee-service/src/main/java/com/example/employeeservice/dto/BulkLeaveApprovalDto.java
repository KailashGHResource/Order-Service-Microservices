package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkLeaveApprovalDto {
    private Long leaveId;
    private String status; // e.g., "APPROVED" or "REJECTED"
    private String managerComments;
}