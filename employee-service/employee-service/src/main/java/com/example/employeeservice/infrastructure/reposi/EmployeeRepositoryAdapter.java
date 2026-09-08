package com.example.employeeservice.infrastructure.reposi;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeFilterDto;
import com.example.employeeservice.infrastructure.entity.EmployeeEntity;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository jpaRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public Optional<Employee> findById(Long id) {
        return jpaRepository.findById(id).map(employeeMapper::toDomain);
    }

    @Override
    public Optional<Employee> findFirstByEmail(String email) {
        return jpaRepository.findFirstByEmail(email).map(employeeMapper::toDomain);
    }

    @Override
    public Employee save(Employee employee) {
        var entity = employeeMapper.toEntity(employee);
        var savedEntity = jpaRepository.save(entity);
        return employeeMapper.toDomain(savedEntity);
    }

    @Override
    public List<Employee> findAll() {
        return jpaRepository.findAll().stream()
                .map(employeeMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Page<Employee> findWithFilters(EmployeeFilterDto filter, Pageable pageable) {
        // 1. Build the dynamic SQL specification
        Specification<EmployeeEntity> spec = EmployeeSpecification.getFilters(filter);

        // 2. Execute query with pagination and sorting (fixed variable name)
        Page<EmployeeEntity> entityPage = jpaRepository.findAll(spec, pageable);

        // 3. Map the DB Entities back to pure Domain models (fixed method name)
        return entityPage.map(entity -> employeeMapper.toDomain(entity));
    }



    // Add this implementation!
    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
