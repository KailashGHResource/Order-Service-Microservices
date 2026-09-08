package com.example.employeeservice.application;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.EmployeeCreatedEventV1;
import com.example.employeeservice.dto.EmployeeRequestDto;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.example.employeeservice.mapper.EmployeeMapper;
import com.example.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    // 1. Mock all dependencies injected into the Service
    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    // 2. Inject the mocks into the actual service instance
    @InjectMocks
    private EmployeeService employeeService;

    private Employee domainEmployee;
    private EmployeeRequestDto requestDto;
    private EmployeeResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Setup mock data to be used across tests
        domainEmployee = new Employee();
        domainEmployee.setId(1L);
        domainEmployee.setFirstName("Clark");
        domainEmployee.setLastName("Kent");
        domainEmployee.setEmail("clark.kent@dailyplanet.com");

        requestDto = new EmployeeRequestDto();
        requestDto.setFirstName("Clark");
        requestDto.setLastName("Kent");
        requestDto.setEmail("clark.kent@dailyplanet.com");

        responseDto = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("Clark")
                .lastName("Kent")
                .email("clark.kent@dailyplanet.com")
                .build();
    }

    @Test
    void givenValidId_whenGetEmployeeById_thenReturnEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(domainEmployee));
        when(employeeMapper.toDto(domainEmployee)).thenReturn(responseDto);

        // Act
        EmployeeResponseDto result = employeeService.getEmployeeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Clark");
        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    void givenInvalidId_whenGetEmployeeById_thenThrowException() {
        // Arrange
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found with ID: 99");
    }

    @Test
    void givenValidRequest_whenCreateEmployee_thenSaveAndPublishEvent() {
        // Arrange
        when(employeeMapper.toDomain(requestDto)).thenReturn(domainEmployee);
        when(employeeRepository.save(domainEmployee)).thenReturn(domainEmployee);
        when(employeeMapper.toDto(domainEmployee)).thenReturn(responseDto);

        // Act
        EmployeeResponseDto result = employeeService.createEmployee(requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("clark.kent@dailyplanet.com");

        // Verify the database save was called
        verify(employeeRepository, times(1)).save(domainEmployee);

        // Verify the RabbitMQ event was published exactly once
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("leave.exchange"),
                eq("employee.created.key"),
                any(EmployeeCreatedEventV1.class)
        );
    }

    @Test
    void givenValidId_whenDeleteEmployee_thenDeleteFromRepository() {
        // Arrange
        when(employeeRepository.existsById(1L)).thenReturn(true);

        // Act
        employeeService.deleteEmployee(1L);

        // Assert
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    void givenInvalidId_whenDeleteEmployee_thenThrowException() {
        // Arrange
        when(employeeRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found with ID: 99");

        // Verify that delete was NEVER called because it threw an exception first
        verify(employeeRepository, never()).deleteById(anyLong());
    }
}