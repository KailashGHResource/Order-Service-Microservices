package com.example.leaveservice.integrationtest;

import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import com.example.leaveservice.infrastructure.repository.LeaveJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        // Point both Feign clients to the dynamic WireMock test port
        "employee-service.url=http://localhost:${wiremock.server.port}",
        "notification-service.url=http://localhost:${wiremock.server.port}"
})
class LeaveIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LeaveJpaRepository leaveRepository; // Injected infrastructure repository for deleteAll support

    private LeaveEntity savedLeave;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();

        LeaveEntity leave = new LeaveEntity();
        leave.setEmployeeId(105L);
        leave.setStartDate(LocalDate.parse("2026-12-20"));
        leave.setEndDate(LocalDate.parse("2026-12-26"));
        leave.setStatus("PENDING");
        savedLeave = leaveRepository.save(leave);
    }

    @Test
    void testGetStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/leaves/status", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Leave Service is up and running on port 8083!", response.getBody());
    }

    @Test
    void testCreateLeaveRequest() {
        LeaveEntity newLeave = new LeaveEntity();
        newLeave.setEmployeeId(105L);
        newLeave.setStartDate(LocalDate.parse("2027-01-10"));
        newLeave.setEndDate(LocalDate.parse("2027-01-15"));

        ResponseEntity<LeaveEntity> response = restTemplate.postForEntity("/api/leaves", newLeave, LeaveEntity.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PENDING", response.getBody().getStatus());
        assertEquals(105L, response.getBody().getEmployeeId());
    }

    @Test
    void testGetLeaveWithEmployee() {
        // 1. STUB: Mock the Employee Client response
        stubFor(get(urlEqualTo("/api/employees/105"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                    "id": 105,
                                    "name": "Alice Wonderland",
                                    "email": "alice@example.com",
                                    "departmentId": 2
                                }
                                """)));

        // 2. EXECUTE: Call Leave Service endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/leaves/" + savedLeave.getId() + "/with-employee",
                Map.class
        );

        // 3. ASSERT
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> employeeData = (Map<String, Object>) response.getBody().get("employeeDetails");
        assertEquals("Alice Wonderland", employeeData.get("name"));
        assertEquals("alice@example.com", employeeData.get("email"));
    }

    @Test
    void testApproveLeaveAndNotify() {
        // 1. STUB: Mock Employee Client
        stubFor(get(urlEqualTo("/api/employees/105"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\": 105, \"name\": \"Alice\", \"email\": \"alice@example.com\", \"departmentId\": 2}")));

        // 2. STUB: Mock Notification Client
        stubFor(post(urlEqualTo("/api/notifications"))
                .willReturn(aResponse().withStatus(200)));

        // 3. EXECUTE: Call the approve endpoint
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/leaves/" + savedLeave.getId() + "/approve",
                HttpMethod.PUT,
                null,
                String.class
        );

        // 4. ASSERT
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Leave Approved and Notification Sent to: alice@example.com", response.getBody());

        // Verify Database actually updated status to APPROVED
        LeaveEntity updatedLeave = leaveRepository.findById(savedLeave.getId()).get();
        assertEquals("APPROVED", updatedLeave.getStatus());
    }
}