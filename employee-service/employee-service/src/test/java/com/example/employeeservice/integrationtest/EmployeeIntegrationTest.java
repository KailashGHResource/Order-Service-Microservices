package com.example.employeeservice.integrationtest;

import com.example.employeeservice.client.DepartmentClient;
import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.dto.AuthResponse;
import com.example.employeeservice.dto.DepartmentDto;
import com.example.employeeservice.dto.EmployeeRequestDto;
import com.example.employeeservice.dto.EmployeeResponseDto;
import com.example.employeeservice.dto.LoginRequest;
import com.example.employeeservice.dto.EmployeeCreatedEventV1;
import com.example.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private RabbitTemplate rabbitTemplate; // Mocked so we can capture and test published events

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee savedEmployee;

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
    }

    // --- V1 API TESTS ---

    @Test
    void testGetStatusV1() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/employees/status", String.class);
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

        ResponseEntity<EmployeeResponseDto> response = restTemplate.postForEntity("/api/v1/employees", newEmp, EmployeeResponseDto.class);

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

        mockMvc.perform(get("/api/v1/employees/" + savedEmployee.getId() + "/with-department"))
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

        ResponseEntity<EmployeeResponseDto> response = restTemplate.postForEntity(
                "/api/v1/employees", requestDto, EmployeeResponseDto.class
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
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v2/employees/status", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Employee Service V2 is up and running!", response.getBody());
    }

    @Test
    void testSearchEmployeesV2() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v2/employees/search?firstName=John", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}