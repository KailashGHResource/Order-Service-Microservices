package com.example.employeeservice.client;

import com.example.employeeservice.dto.DepartmentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DepartmentClientFallback implements DepartmentClient {

    private static final Logger log = LoggerFactory.getLogger(DepartmentClientFallback.class);

    @Override
    public DepartmentDto getDepartmentById(Long id) {
        log.warn("Department Service is down or unreachable. Fallback executed for department ID: {}", id);

        // Return a controlled default response
        DepartmentDto fallback = new DepartmentDto();

        // Adjust these setters based on your actual DepartmentDto fields
        fallback.setId(id);
        // fallback.setName("Department Unavailable (Fallback)");

        return fallback;
    }
}