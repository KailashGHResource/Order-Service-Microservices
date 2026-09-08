package com.example.employeeservice.security;

import com.example.employeeservice.application.EmployeeService;
import com.example.employeeservice.dto.EmployeeRequestDto;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // The IDE might say "never assigned", but Spring Boot's @MockBean assigns it at runtime. You can ignore that warning!
    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenNoToken_whenGetAllEmployees_thenUnauthorized401() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenEmployeeRole_whenDeleteEmployee_thenForbidden403() throws Exception {
        String token = jwtTokenProvider.generateTestToken("regular_emp", List.of("EMPLOYEE"));

        mockMvc.perform(delete("/api/v1/employees/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenHrRole_whenCreateEmployee_thenSuccess201() throws Exception {
        String token = jwtTokenProvider.generateTestToken("hr_personnel", List.of("HR"));

        EmployeeRequestDto dto = new EmployeeRequestDto();
        dto.setFirstName("Clark");
        dto.setLastName("Kent");
        dto.setEmail("clark@dailyplanet.com");

        EmployeeResponseDto mockResponse = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("Clark")
                .lastName("Kent")
                .email("clark@dailyplanet.com")
                .build();

        // THIS IS THE CRITICAL FIX: It MUST say .saveEmployee (matching the controller)
        when(employeeService.saveEmployee(any(EmployeeRequestDto.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Clark"));
    }

    @Test
    void givenInvalidPayload_whenCreateEmployee_thenBadRequest400() throws Exception {
        String token = jwtTokenProvider.generateTestToken("hr_personnel", List.of("HR"));

        EmployeeRequestDto invalidDto = new EmployeeRequestDto();
        invalidDto.setFirstName(""); // This will now fail the @NotBlank rule!
        invalidDto.setEmail("not-an-email"); // This will now fail the @Email rule!

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenEmployeeRecord_whenGetEmployee_thenSensitiveDataIsExcluded() throws Exception {
        String token = jwtTokenProvider.generateTestToken("regular_emp", List.of("EMPLOYEE"));

        EmployeeResponseDto mockResponse = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("Bruce")
                .lastName("Wayne")
                .email("bruce@wayne.com")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/employees/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.salary").doesNotExist());
    }
}