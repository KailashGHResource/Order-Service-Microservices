package com.example.departmentservice.infrastructure.repository;

import com.example.departmentservice.domain.Department;
import com.example.departmentservice.mapper.DepartmentMapper;
import com.example.departmentservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DepartmentRepositoryAdapter implements DepartmentRepository {

    private final DepartmentJpaRepository jpaRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public Department save(Department department) {
        var entity = departmentMapper.toEntity(department);
        var savedEntity = jpaRepository.save(entity);
        return departmentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Department> findById(Long id) {
        return jpaRepository.findById(id).map(departmentMapper::toDomain);
    }

    @Override
    public List<Department> findAll() {
        return jpaRepository.findAll().stream()
                .map(departmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}