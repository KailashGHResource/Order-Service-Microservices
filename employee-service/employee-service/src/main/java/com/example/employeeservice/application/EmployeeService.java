package com.example.employeeservice.application;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeRequestDto;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.example.employeeservice.dto.EmployeeFilterDto;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.employeeservice.dto.BulkOperationResponse;
import com.example.employeeservice.dto.BulkLeaveApprovalDto; // Added Import
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.employeeservice.dto.EmployeeCreatedEventV1;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.UUID;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final RabbitTemplate rabbitTemplate;

    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findFirstByEmail(email);
    }

    public EmployeeResponseDto saveEmployee(EmployeeRequestDto requestDto) {
        return createEmployee(requestDto);
    }

    public List<EmployeeResponseDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "employees", key = "#id")
    public EmployeeResponseDto getEmployeeById(Long id) {
        log.info("--> DB FETCH EXECUTED (Cache Miss) for Employee ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", id);
                    return new RuntimeException("Employee not found with ID: " + id);
                });
        return employeeMapper.toDto(employee);
    }

    @CachePut(value = "employees", key = "#id")
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto updatedDetails) {
        log.info("Updating employee details for ID: {} in Database and refreshing Redis cache", id);

        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        existingEmployee.setFirstName(updatedDetails.getFirstName());
        existingEmployee.setLastName(updatedDetails.getLastName());
        existingEmployee.setEmail(updatedDetails.getEmail());

        // ---> ADDED: Map the version from the client for Optimistic Locking <---
        if (updatedDetails.getVersion() != null) {
            existingEmployee.setVersion(updatedDetails.getVersion());
        }

        if (updatedDetails.getPassword() != null) {
            existingEmployee.setPassword(updatedDetails.getPassword());
        }
        if (updatedDetails.getDepartmentId() != null) {
            existingEmployee.setDepartmentId(updatedDetails.getDepartmentId());
        }

        Employee savedEmployee = employeeRepository.save(existingEmployee);
        return employeeMapper.toDto(savedEmployee);
    }

    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        log.info("Request received to delete Employee ID: {} from Database and wipe from Redis cache", id);
        if (!employeeRepository.existsById(id)) {
            log.error("Cannot delete. Employee not found with ID: {}", id);
            throw new RuntimeException("Employee not found with ID: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public Page<Employee> searchEmployees(EmployeeFilterDto filter, Pageable pageable) {
        return employeeRepository.findWithFilters(filter, pageable);
    }

    public EmployeeResponseDto createEmployee(EmployeeRequestDto requestDto) {
        // 1. Map to Domain and Save to DB
        Employee domain = employeeMapper.toDomain(requestDto);
        Employee savedEmployee = employeeRepository.save(domain);

        // 2. Build Versioned Event
        EmployeeCreatedEventV1 event = EmployeeCreatedEventV1.builder()
                .eventId(UUID.randomUUID().toString())
                .employeeId(savedEmployee.getId())
                .firstName(savedEmployee.getFirstName())
                .lastName(savedEmployee.getLastName())
                .email(savedEmployee.getEmail())
                .timestamp(LocalDateTime.now())
                .build();

        // 3. Publish to RabbitMQ Topic Exchange
        log.info("Publishing EmployeeCreatedEventV1 for Employee ID: {}", savedEmployee.getId());

        rabbitTemplate.convertAndSend("leave.exchange", "employee.created.key", event);

        return employeeMapper.toDto(savedEmployee);
    }

    // --- DAY 4: BULK CREATION WITH PARTIAL FAILURE HANDLING ---
    public BulkOperationResponse createEmployeesBulk(List<EmployeeRequestDto> requests) {
        log.info("Starting bulk creation for {} employees", requests.size());

        List<Object> successes = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();

        for (EmployeeRequestDto requestDto : requests) {
            String identifier = requestDto.getEmail() != null ? requestDto.getEmail() : "Unknown Email";
            try {
                if (employeeRepository.findFirstByEmail(requestDto.getEmail()).isPresent()) {
                    errors.put(identifier, "Email already exists in the system.");
                    continue;
                }
                EmployeeResponseDto savedEmployee = createEmployee(requestDto);
                successes.add(savedEmployee);

            } catch (Exception e) {
                log.error("Failed to create employee in bulk batch [{}]: {}", identifier, e.getMessage());
                errors.put(identifier, e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .successfulCount(successes.size())
                .failedCount(errors.size())
                .successes(successes)
                .errors(errors)
                .build();
    }

    // --- DAY 4: BULK LEAVE APPROVAL WITH PARTIAL FAILURES ---
    public BulkOperationResponse processBulkLeaveApprovals(List<BulkLeaveApprovalDto> approvalRequests) {
        log.info("Starting bulk leave approval for {} requests", approvalRequests.size());

        List<Object> successes = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();

        for (BulkLeaveApprovalDto request : approvalRequests) {
            String identifier = "Leave ID: " + request.getLeaveId();

            try {
                // SIMULATED DATABASE CHECK FOR TESTING:
                // We force ID 999 to fail to prove partial failure handling works.
                if (request.getLeaveId() == 999) {
                    throw new RuntimeException("Leave request not found in database.");
                }

                Map<String, Object> simulatedSavedLeave = new HashMap<>();
                simulatedSavedLeave.put("leaveId", request.getLeaveId());
                simulatedSavedLeave.put("status", request.getStatus().toUpperCase());
                simulatedSavedLeave.put("managerComments", request.getManagerComments());

                successes.add(simulatedSavedLeave);

                log.info("Successfully processed {}", identifier);

            } catch (Exception e) {
                log.error("Failed to process {}: {}", identifier, e.getMessage());
                errors.put(identifier, e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .successfulCount(successes.size())
                .failedCount(errors.size())
                .successes(successes)
                .errors(errors)
                .build();
    }
}