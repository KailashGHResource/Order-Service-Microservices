package com.example.departmentservice.security;
import com.example.departmentservice.security.JwtTokenProvider;
import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.application.DepartmentService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenNoToken_whenGetDepartments_thenUnauthorized401() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenEmployeeRole_whenCreateDepartment_thenForbidden403() throws Exception {
        String token = jwtTokenProvider.generateTestToken("emp_user", List.of("EMPLOYEE"));
        DepartmentRequestDto dto = new DepartmentRequestDto();
        dto.setName("HR");

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminRole_whenCreateDepartment_thenSuccess201() throws Exception {
        String token = jwtTokenProvider.generateTestToken("admin_user", List.of("ADMIN"));
        DepartmentRequestDto dto = new DepartmentRequestDto();
        dto.setName("Engineering");
        dto.setCode("ENG");

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void givenManagerRole_whenGetDepartments_thenSuccess200() throws Exception {
        String token = jwtTokenProvider.generateTestToken("manager_user", List.of("MANAGER"));

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}