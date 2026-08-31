package com.example.departmentservice.repository;

import com.example.departmentservice.domain.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Department save(Department department);
    Optional<Department> findById(Long id);
    List<Department> findAll();
    void deleteById(Long id);
}