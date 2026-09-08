package com.example.employeeservice.infrastructure.reposi;

import com.example.employeeservice.infrastructure.entity.EmployeeEntity;
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
class EmployeeJpaRepositoryTest { // Renamed slightly for clarity

    @Autowired
    private EmployeeJpaRepository employeeJpaRepository;

    private EmployeeEntity testEmployee;

    @BeforeEach
    void setUp() {
        // Setup data using the Entity, not the Domain object
        testEmployee = new EmployeeEntity();
        testEmployee.setFirstName("Bruce");
        testEmployee.setLastName("Wayne");
        testEmployee.setEmail("bruce.wayne@wayneenterprises.com");
        testEmployee.setDepartmentId(2L);
        employeeJpaRepository.save(testEmployee);
    }

    @AfterEach
    void tearDown() {
        employeeJpaRepository.deleteAll();
    }

    @Test
    void givenExistingEmail_whenFindFirstByEmail_thenReturnEmployee() {
        // Act
        Optional<EmployeeEntity> foundEmployee = employeeJpaRepository.findFirstByEmail("bruce.wayne@wayneenterprises.com");

        // Assert
        assertThat(foundEmployee).isPresent();
        assertThat(foundEmployee.get().getFirstName()).isEqualTo("Bruce");
    }

    @Test
    void givenNonExistingEmail_whenFindFirstByEmail_thenReturnEmpty() {
        // Act
        Optional<EmployeeEntity> foundEmployee = employeeJpaRepository.findFirstByEmail("unknown@domain.com");

        // Assert
        assertThat(foundEmployee).isNotPresent();
    }
}