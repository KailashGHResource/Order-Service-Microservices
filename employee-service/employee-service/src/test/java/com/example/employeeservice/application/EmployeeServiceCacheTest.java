package com.example.employeeservice.application;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "services.notification.url=http://localhost:8083",
        "services.department.url=http://localhost:8082",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/employee_db_test",
        "spring.datasource.username=admin",
        "spring.datasource.password=admin",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.config.import=" // Disables the config server for this isolated test
})
class EmployeeServiceCacheTest {

    @Autowired
    private EmployeeService employeeService; // <--- THIS WAS MISSING!

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private EmployeeMapper employeeMapper;

    @BeforeEach
    void setUp() {
        // Clear the cache before each test to ensure a clean slate
        cacheManager.getCache("employees").clear();
    }

    @Test
    void givenEmployeeInDb_whenGetEmployeeByIdTwice_thenDbIsCalledOnlyOnce() {
        // Arrange
        Long employeeId = 1L;
        Employee mockEmployee = new Employee();
        mockEmployee.setId(employeeId);

        EmployeeResponseDto mockResponse = new EmployeeResponseDto();
        mockResponse.setId(employeeId);
        mockResponse.setFirstName("John");

        // We tell the mock database to return our employee when asked
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(employeeMapper.toDto(mockEmployee)).thenReturn(mockResponse);

        // Act 1: First call (Should miss cache and hit the database)
        EmployeeResponseDto response1 = employeeService.getEmployeeById(employeeId);

        // Act 2: Second call (Should hit cache and SKIP the database)
        EmployeeResponseDto response2 = employeeService.getEmployeeById(employeeId);

        // Assert
        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        assertThat(response1.getId()).isEqualTo(response2.getId());

        // PROOF OF PERFORMANCE: Verify the database was only queried ONCE, despite two method calls!
        verify(employeeRepository, times(1)).findById(employeeId);

        // Verify the mapper was only called once (since the second time, the already-mapped DTO came from cache)
        verify(employeeMapper, times(1)).toDto(mockEmployee);
    }
}