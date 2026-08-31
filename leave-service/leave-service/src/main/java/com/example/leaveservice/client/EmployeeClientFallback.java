package com.example.leaveservice.client;

import com.example.leaveservice.dto.EmployeeDto;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EmployeeClientFallback implements EmployeeClient {

    @Override
    public EmployeeDto getEmployeeById(Long id) {
        // We must ABORT the transaction because we cannot verify the employee.
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Employee Service is currently offline. Cannot verify employee for leave request."
        );
    }
}