package com.example.employeeservice.client;

import com.example.employeeservice.dto.DepartmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "department-service",
        url = "${services.department.url:http://localhost:8082}",
        fallback = DepartmentClientFallback.class
)
public interface DepartmentClient {

    @GetMapping("/api/departments/{id}")
    DepartmentDto getDepartmentById(@PathVariable("id") Long id);
}