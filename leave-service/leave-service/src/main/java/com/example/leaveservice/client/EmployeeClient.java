package com.example.leaveservice.client;

import com.example.leaveservice.dto.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "employee-service",
        url = "${services.employee.url:http://localhost:8081}",
        fallback = EmployeeClientFallback.class
)
public interface EmployeeClient {

    @GetMapping("/api/v1/employees/{id}")
    EmployeeDto getEmployeeById(@PathVariable("id") Long id);
}