package com.example.employeeservice.repository;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Optional<Employee> findById(Long id);

    // ---> RENAMED THIS METHOD <---
    Optional<Employee> findFirstByEmail(String email);

    Employee save(Employee employee);
    List<Employee> findAll();
    boolean existsById(Long id);
    void deleteById(Long id);
    void deleteAll();
    Page<Employee> findWithFilters(EmployeeFilterDto filter, Pageable pageable);
}