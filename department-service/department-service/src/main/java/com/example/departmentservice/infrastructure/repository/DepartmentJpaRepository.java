package com.example.departmentservice.infrastructure.repository;
import com.example.departmentservice.infrastructure.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Long> {
}