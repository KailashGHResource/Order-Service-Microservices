package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse implements Serializable {
    private int successfulCount;
    private int failedCount;
    private List<Object> successes; // <-- Changed from List<EmployeeResponseDto> to List<Object>
    private Map<String, String> errors;
}