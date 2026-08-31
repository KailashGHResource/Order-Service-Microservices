package com.example.leaveservice.infrastructure.repository;

import com.example.leaveservice.dto.LeaveFilterDto;
import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class LeaveSpecification {

    public static Specification<LeaveEntity> getFilters(LeaveFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getLeaveType() != null && !filter.getLeaveType().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("leaveType")),
                        filter.getLeaveType().toUpperCase()
                ));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("status")),
                        filter.getStatus().toUpperCase()
                ));
            }

            // Date Range Filtering
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), filter.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}