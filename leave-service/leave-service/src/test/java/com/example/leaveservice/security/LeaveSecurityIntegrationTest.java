package com.example.leaveservice.security;

import com.example.leaveservice.application.LeaveService;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.dto.LeaveResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private LeaveService leaveService;

    // Bypassing Redis connections for tests
    @MockBean
    private org.redisson.api.RedissonClient redissonClient;

    @MockBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    // ---> NEW FIX: Also mock the Reactive connection factory <---
    @MockBean
    private org.springframework.data.redis.connection.ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    // -----------------------------------------------------------

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenNoToken_whenGetLeaves_thenUnauthorized401() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenEmployeeRole_whenCreateLeaveRequest_thenSuccess201() throws Exception {
        String token = jwtTokenProvider.generateTestToken("employee_user", List.of("EMPLOYEE"));

        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setEmployeeId(1L);
        dto.setStartDate(LocalDate.of(2026, 10, 1));
        dto.setEndDate(LocalDate.of(2026, 10, 5));

        LeaveResponseDto mockResponse = LeaveResponseDto.builder()
                .id(10L)
                .status("PENDING")
                .build();

        when(leaveService.createLeaveRequest(any(LeaveRequestDto.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/leaves")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void givenEmployeeRole_whenApproveLeave_thenForbidden403() throws Exception {
        String token = jwtTokenProvider.generateTestToken("employee_user", List.of("EMPLOYEE"));

        mockMvc.perform(put("/api/v1/leaves/bulk-approval")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(10L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenManagerRole_whenApproveLeave_thenSuccess() throws Exception {
        String token = jwtTokenProvider.generateTestToken("manager_user", List.of("MANAGER"));

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("successful", List.of(10L));
        mockResponse.put("errors", new HashMap<>());

        when(leaveService.bulkApproveLeaves(any(List.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/leaves/bulk-approval")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(10L))))
                .andExpect(status().is2xxSuccessful());
    }
}