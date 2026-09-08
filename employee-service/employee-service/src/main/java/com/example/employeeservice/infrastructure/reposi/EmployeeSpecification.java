package com.example.employeeservice.infrastructure.reposi;

import com.example.employeeservice.dto.EmployeeFilterDto;
import com.example.employeeservice.infrastructure.entity.EmployeeEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<EmployeeEntity> getFilters(EmployeeFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by Name (Partial match, case-insensitive)
            if (filter.getName() != null && !filter.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.getName().toLowerCase() + "%"
                ));
            }

            // 2. Filter by Email (Partial match, case-insensitive)
            if (filter.getEmail() != null && !filter.getEmail().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + filter.getEmail().toLowerCase() + "%"
                ));
            }

            // 3. Filter by Department ID (Exact match)
            if (filter.getDepartmentId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("departmentId"),
                        filter.getDepartmentId()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}