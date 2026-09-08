package com.example.departmentservice.infrastructure.repository;

import com.example.departmentservice.infrastructure.entity.DepartmentEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DepartmentJpaRepositoryTest {

    @Autowired
    private DepartmentJpaRepository departmentJpaRepository;

    private DepartmentEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new DepartmentEntity();
        testEntity.setName("Human Resources");
        testEntity.setDescription("HR Department");
        testEntity.setCode("HR");
        departmentJpaRepository.save(testEntity);
    }

    @AfterEach
    void tearDown() {
        departmentJpaRepository.deleteAll();
    }

    @Test
    void givenSavedDepartment_whenFindById_thenReturnDepartment() {
        // Act
        Optional<DepartmentEntity> found = departmentJpaRepository.findById(testEntity.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Human Resources");
        assertThat(found.get().getCode()).isEqualTo("HR");
    }
}