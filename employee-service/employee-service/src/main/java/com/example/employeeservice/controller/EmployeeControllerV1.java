package com.example.employeeservice.controller;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.*;
import com.example.employeeservice.application.EmployeeService;
import com.example.employeeservice.client.DepartmentClient;
import com.example.employeeservice.client.NotificationClient;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.security.JwtTokenProvider; // <--- ADDED IMPORT
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@Slf4j
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeControllerV1 {

    private final EmployeeService employeeService;
    private final DepartmentClient departmentClient;
    private final NotificationClient notificationClient;
    private final EmployeeMapper employeeMapper;
    private final JwtTokenProvider jwtTokenProvider; // <--- ADDED INJECTION

    @GetMapping("/status")
    public String getStatus() {
        return "Employee Service is up and running on port 8081!";
    }

    @PostMapping("/login")
    @RateLimiter(name = "default", fallbackMethod = "loginRateLimiterFallback")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        return employeeService.findByEmail(loginRequest.getEmail())
                .filter(employee -> employee.getPassword() != null && employee.getPassword().equals(loginRequest.getPassword()))
                .map(employee -> {
                    log.info("Login successful for Employee ID: {}", employee.getId());

                    // ---> UPDATED: Generate a real signed JWT token instead of a dummy string <---
                    List<String> roles = List.of("ADMIN"); // Assigning ADMIN role for seamless testing
                    String token = jwtTokenProvider.generateTestToken(employee.getEmail(), roles);

                    String fullName = employee.getFirstName() + " " + (employee.getLastName() != null ? employee.getLastName() : "");
                    return ResponseEntity.ok(new AuthResponse(token, "Login successful!", employee.getId(), fullName.trim(), employee.getEmail()));
                })
                .orElseGet(() -> {
                    log.warn("Failed login attempt for email: {}", loginRequest.getEmail());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new AuthResponse(null, "Invalid email or password", null, null, null));
                });
    }

    @PostMapping
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeRequestDto requestDto) {
        log.info("Creating new employee with email: {}", requestDto.getEmail());
        EmployeeResponseDto savedEmployee = employeeService.saveEmployee(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        log.info("Received HTTP GET request for Employee ID: {}", id);
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/{id}/with-department")
    @RateLimiter(name = "default", fallbackMethod = "departmentRateLimiterFallback")
    public ResponseEntity<Map<String, Object>> getEmployeeWithDepartment(@PathVariable Long id) {
        log.info("Fetching employee and department details for Employee ID: {}", id);

        EmployeeResponseDto employee = employeeService.getEmployeeById(id);

        DepartmentDto department = null;
        if (employee.getDepartmentId() != null) {
            department = departmentClient.getDepartmentById(employee.getDepartmentId());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("employee", employee);
        response.put("department", department);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequestDto updatedDetails) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, updatedDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee with ID " + id + " deleted successfully.");
    }

    // --- FALLBACK METHODS ---

    public ResponseEntity<Map<String, Object>> departmentRateLimiterFallback(Long id, Throwable t) {
        log.warn("Rate limit triggered for department fetch on Employee ID: {}", id);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "HTTP 429 Too Many Requests");
        response.put("message", "Rate limit exceeded! Please slow down your requests.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    public ResponseEntity<?> loginRateLimiterFallback(LoginRequest loginRequest, Throwable t) {
        log.warn("Rate limit triggered for login attempt on email: {}", loginRequest.getEmail());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "HTTP 429 Too Many Requests");
        response.put("message", "Too many login attempts. Please wait.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponseDto>> searchEmployees(
            @ModelAttribute EmployeeFilterDto filter,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {

        Page<Employee> domainPage = employeeService.searchEmployees(filter, pageable);
        Page<EmployeeResponseDto> responsePage = domainPage.map(employeeMapper::toDto);

        return ResponseEntity.ok(responsePage);
    }

    // --- DAY 4: BULK EMPLOYEE CREATION ---
    @PostMapping("/bulk")
    public ResponseEntity<BulkOperationResponse> createEmployeesBulk(@RequestBody List<EmployeeRequestDto> requestDtos) {
        log.info("Received REST request for bulk employee creation (Count: {})", requestDtos.size());
        BulkOperationResponse response = employeeService.createEmployeesBulk(requestDtos);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
    }

    // --- DAY 4: BULK LEAVE APPROVAL ---
    @PostMapping("/leaves/bulk-approval")
    public ResponseEntity<BulkOperationResponse> approveLeavesBulk(@RequestBody List<BulkLeaveApprovalDto> requests) {
        log.info("Received REST request for bulk leave approval (Count: {})", requests.size());
        BulkOperationResponse response = employeeService.processBulkLeaveApprovals(requests);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
    }
}