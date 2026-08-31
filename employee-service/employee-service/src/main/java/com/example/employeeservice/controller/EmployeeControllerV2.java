package com.example.employeeservice.controller;

import com.example.employeeservice.application.EmployeeService;
import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeFilterDto;
import com.example.employeeservice.dto.EmployeeResponseDtoV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/employees") // V2 API Path
@RequiredArgsConstructor
public class EmployeeControllerV2 {

    private final EmployeeService employeeService;

    @GetMapping("/status")
    public String getStatusV2() {
        return "Employee Service V2 is up and running!";
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponseDtoV2>> searchEmployeesV2(
            @ModelAttribute EmployeeFilterDto filter,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {

        log.info("V2 Search requested");
        Page<Employee> domainPage = employeeService.searchEmployees(filter, pageable);

        // Map pure Domain models to V2 Response DTOs
        Page<EmployeeResponseDtoV2> responsePage = domainPage.map(this::mapToV2Dto);

        return ResponseEntity.ok(responsePage);
    }

    // Helper method to map Domain -> V2 DTO
    private EmployeeResponseDtoV2 mapToV2Dto(Employee employee) {
        // ---> FIXED: We no longer need to split strings! We map directly. <---
        return EmployeeResponseDtoV2.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .departmentId(employee.getDepartmentId())
                .build();
    }
}