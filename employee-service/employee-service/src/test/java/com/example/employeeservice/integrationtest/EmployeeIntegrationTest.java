package com.example.employeeservice.integrationtest;

import com.example.employeeservice.client.DepartmentClient;
import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.*;
import com.example.employeeservice.repository.EmployeeRepository;
import com.example.employeeservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "department-service.url=http://localhost:${wiremock.server.port}",
        "services.notification.url=http://localhost:${wiremock.server.port}"
})
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentClient departmentClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee savedEmployee;
    private String adminToken;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();

        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@example.com");
        employee.setPassword("secret123");
        employee.setDepartmentId(99L);
        savedEmployee = employeeRepository.save(employee);

        // Generate a valid JWT for secured test requests
        adminToken = jwtTokenProvider.generateTestToken("admin@example.com", List.of("ADMIN", "HR"));
    }

    private HttpEntity<Object> createAuthHeader(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> createAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        return new HttpEntity<>(headers);
    }

    // --- V1 API TESTS ---

    @Test
    void testGetStatusV1() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/employees/status", HttpMethod.GET, createAuthHeader(), String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Employee Service is up and running on port 8081!", response.getBody());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest login = new LoginRequest();
        login.setEmail("john@example.com");
        login.setPassword("secret123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/v1/employees/login", login, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Login successful!", response.getBody().getMessage());
        assertNotNull(response.getBody().getToken());
    }

    @Test
    void testCreateEmployeeWithWelcomeEmail() {
        stubFor(post(urlEqualTo("/api/notifications"))
                .willReturn(aResponse().withStatus(200)));

        EmployeeRequestDto newEmp = new EmployeeRequestDto();
        newEmp.setFirstName("Alice");
        newEmp.setLastName("Smith");
        newEmp.setEmail("alice@example.com");
        newEmp.setPassword("password123");
        newEmp.setDepartmentId(1L);

        ResponseEntity<EmployeeResponseDto> response = restTemplate.exchange(
                "/api/v1/employees", HttpMethod.POST, createAuthHeader(newEmp), EmployeeResponseDto.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Alice", response.getBody().getFirstName());
        assertEquals("Smith", response.getBody().getLastName());
    }

    @Test
    void testGetEmployeeWithDepartment() throws Exception {
        DepartmentDto mockDepartment = new DepartmentDto();
        mockDepartment.setId(99L);
        mockDepartment.setDepartmentName("Engineering Mock");

        when(departmentClient.getDepartmentById(99L)).thenReturn(mockDepartment);

        mockMvc.perform(get("/api/v1/employees/" + savedEmployee.getId() + "/with-department")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.firstName").value("John"))
                .andExpect(jsonPath("$.employee.lastName").value("Doe"))
                .andExpect(jsonPath("$.department.departmentName").value("Engineering Mock"));
    }

    @Test
    void testEmployeeCreatedEventCompatibility() {
        stubFor(post(urlEqualTo("/api/notifications"))
                .willReturn(aResponse().withStatus(200)));

        EmployeeRequestDto requestDto = new EmployeeRequestDto();
        requestDto.setFirstName("EventTest");
        requestDto.setLastName("User");
        requestDto.setEmail("event.test@example.com");
        requestDto.setPassword("secure123");
        requestDto.setDepartmentId(1L);

        ResponseEntity<EmployeeResponseDto> response = restTemplate.exchange(
                "/api/v1/employees", HttpMethod.POST, createAuthHeader(requestDto), EmployeeResponseDto.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        org.mockito.ArgumentCaptor<EmployeeCreatedEventV1> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(EmployeeCreatedEventV1.class);

        org.mockito.Mockito.verify(rabbitTemplate, org.mockito.Mockito.atLeastOnce())
                .convertAndSend(
                        org.mockito.Mockito.eq("leave.exchange"),
                        org.mockito.Mockito.eq("employee.created.key"),
                        eventCaptor.capture()
                );

        EmployeeCreatedEventV1 capturedEvent = eventCaptor.getValue();

        assertNotNull(capturedEvent.getEventId(), "Event ID must not be null for traceability");
        assertNotNull(capturedEvent.getTimestamp(), "Timestamp must be included in V1 schema");
        assertEquals(response.getBody().getId(), capturedEvent.getEmployeeId());
        assertEquals("EventTest", capturedEvent.getFirstName());
        assertEquals("User", capturedEvent.getLastName());
        assertEquals("event.test@example.com", capturedEvent.getEmail());
    }

    // --- V2 API TESTS ---

    @Test
    void testGetStatusV2() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v2/employees/status", HttpMethod.GET, createAuthHeader(), String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Employee Service V2 is up and running!", response.getBody());
    }

    @Test
    void testSearchEmployeesV2() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v2/employees/search?firstName=John", HttpMethod.GET, createAuthHeader(), String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}