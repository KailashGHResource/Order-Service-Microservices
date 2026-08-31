package com.example.auditservice.integrationtest;
import com.example.auditservice.domain.AuditLog;
import com.example.auditservice.event.EmployeeCreatedEvent;
import com.example.auditservice.listener.AuditEventListener;
import com.example.auditservice.repository.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Forces Spring to use application-test.properties
class AuditServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Used to simulate HTTP GET requests to your controller

    @Autowired
    private AuditEventListener auditEventListener; // The RabbitMQ listener

    @Autowired
    private AuditRepository auditRepository; // The database repository

    @BeforeEach
    void setUp() {
        // Clean up the test database before every test to ensure a clean slate
        auditRepository.deleteAll();
    }

    @Test
    void testAuditLogIsSavedAndRetrievedSuccessfully() throws Exception {
        // ---------------------------------------------------------
        // 1. ARRANGE: Create a mock event that RabbitMQ would normally deliver
        // ---------------------------------------------------------
        EmployeeCreatedEvent event = new EmployeeCreatedEvent();
        event.setEmployeeId(999L);
        event.setFirstName("Bruce");
        event.setLastName("Wayne");
        event.setEmployeeEmail("bruce.wayne@example.com");
        event.setDepartment("Security");

        // ---------------------------------------------------------
        // 2. ACT: Simulate RabbitMQ delivering the message to the listener
        // ---------------------------------------------------------
        auditEventListener.handleEmployeeCreated(event);

        // ---------------------------------------------------------
        // 3. ASSERT DB: Verify the event was successfully saved to PostgreSQL
        // ---------------------------------------------------------
        List<AuditLog> logsInDatabase = auditRepository.findAll();
        assertThat(logsInDatabase).hasSize(1); // Ensure exactly 1 log was saved

        AuditLog savedLog = logsInDatabase.get(0);
        assertThat(savedLog.getEventType()).isEqualTo("EMPLOYEE_CREATED");
        assertThat(savedLog.getPayload()).contains("bruce.wayne@example.com"); // Verifies payload captured data

        // ---------------------------------------------------------
        // 4. ASSERT API: Verify the REST endpoint returns the saved log
        // ---------------------------------------------------------
        mockMvc.perform(get("/api/audit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventType").value("EMPLOYEE_CREATED"))
                .andExpect(jsonPath("$[0].payload").exists());
    }
}