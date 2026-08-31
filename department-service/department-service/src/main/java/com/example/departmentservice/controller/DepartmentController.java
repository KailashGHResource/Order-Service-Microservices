package com.example.departmentservice.controller;

import com.example.departmentservice.application.DepartmentService;
import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.dto.DepartmentResponseDto;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/departments") // Upgraded to V1 standard
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/status")
    public String getStatus() {
        log.info("Received request to check Department Service status");
        return "Department Service V1 is up and running on port 8082!";
    }

    // 1. Department CRUD - Create
    @PostMapping
    public ResponseEntity<DepartmentResponseDto> createDepartment(@RequestBody DepartmentRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(requestDto));
    }

    // 2. Department CRUD - Read All
    @GetMapping
    public ResponseEntity<List<DepartmentResponseDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    // 3. Department CRUD - Read by ID + Rate Limiting
    @GetMapping("/{id}")
    @RateLimiter(name = "default", fallbackMethod = "departmentRateLimiterFallback")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    // 4. Department CRUD - Update
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(@PathVariable Long id, @RequestBody DepartmentRequestDto updatedDetails) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, updatedDetails));
    }

    // 5. Department CRUD - Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok("Department with ID " + id + " deleted successfully.");
    }

    // 6. Department Assignment Validation
    @PutMapping("/{departmentId}/assign-employee/{employeeId}")
    public ResponseEntity<String> assignDepartmentToEmployee(@PathVariable Long departmentId, @PathVariable Long employeeId) {
        log.info("Request to validate assignment of Department ID: {} to Employee ID: {}", departmentId, employeeId);

        departmentService.validateDepartmentExists(departmentId);

        log.info("Successfully validated assignment of Department {} to Employee {}", departmentId, employeeId);
        return ResponseEntity.ok("Department " + departmentId + " assigned to Employee " + employeeId + " successfully!");
    }

    // --- FALLBACK METHOD FOR RATE LIMITING ---

    public ResponseEntity<DepartmentResponseDto> departmentRateLimiterFallback(Long id, Throwable t) {
        log.warn("Rate limit triggered for department fetch on Department ID: {}. Reason: {}", id, t.getMessage());

        // Return a DTO instead of a Database Entity
        DepartmentResponseDto fallbackDto = DepartmentResponseDto.builder()
                .id(id)
                .name("Rate Limit Exceeded")
                .description("Too many requests to Department Service. Please slow down and try again after 10 seconds.")
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(fallbackDto);
    }
}