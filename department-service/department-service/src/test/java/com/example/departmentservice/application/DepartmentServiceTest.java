package com.example.departmentservice.application;

import com.example.departmentservice.domain.Department;
import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.dto.DepartmentResponseDto;
import com.example.departmentservice.mapper.DepartmentMapper;
import com.example.departmentservice.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private DepartmentRequestDto requestDto;
    private Department department;
    private DepartmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = new DepartmentRequestDto();
        requestDto.setName("Engineering");
        requestDto.setDescription("Software & Tech");
        requestDto.setCode("ENG");

        department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setDescription("Software & Tech");
        department.setCode("ENG");

        // Using default constructor and setters to prevent constructor access visibility errors
        responseDto = new DepartmentResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Engineering");
        responseDto.setDescription("Software & Tech");
        responseDto.setCode("ENG");
    }

    @Test
    void givenValidRequest_whenCreateDepartment_thenSuccess() {
        // Arrange
        when(departmentMapper.toDomain(requestDto)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // Act
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Engineering");
        verify(departmentRepository, times(1)).save(department);
    }

    @Test
    void whenGetAllDepartments_thenReturnList() {
        // Arrange
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // Act
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Assert
        assertThat(result).hasSize(1);
        // Fixed: Replaced get(0) with Java 21 getFirst()
        assertThat(result.getFirst().getName()).isEqualTo("Engineering");
        verify(departmentRepository, times(1)).findAll();
    }

    @Test
    void givenValidId_whenGetDepartmentById_thenReturnDepartment() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // Act
        DepartmentResponseDto result = departmentService.getDepartmentById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void givenInvalidId_whenGetDepartmentById_thenThrowException() {
        // Arrange
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.getDepartmentById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Department not found with ID: 99");
    }

    @Test
    void givenValidIdAndDto_whenUpdateDepartment_thenSuccess() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // Act
        DepartmentResponseDto result = departmentService.updateDepartment(1L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        verify(departmentRepository, times(1)).save(department);
    }

    @Test
    void givenValidId_whenDeleteDepartment_thenSuccess() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        doNothing().when(departmentRepository).deleteById(1L);

        // Act
        departmentService.deleteDepartment(1L);

        // Assert
        verify(departmentRepository, times(1)).deleteById(1L);
    }
}