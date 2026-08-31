package com.example.leaveservice.client;

import com.example.leaveservice.dto.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Option A: Using Eureka Service Discovery + Resilience Fallback (Recommended)
@FeignClient(
        name = "employee-service",
        url = "${services.employee.url:http://localhost:8081}", // Matches your properties file
        fallback = EmployeeClientFallback.class                  // Enables Day 2 Fallback logic
)
public interface EmployeeClient {

    @GetMapping("/api/employees/{id}")
    EmployeeDto getEmployeeById(@PathVariable("id") Long id);
}