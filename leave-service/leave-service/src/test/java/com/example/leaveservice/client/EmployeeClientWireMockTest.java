package com.example.leaveservice.client;

import com.example.leaveservice.dto.EmployeeDto;
import com.example.leaveservice.dto.NotificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.health.redis.enabled=false",
        // ---> THE FIX: Override the exact variables used in your @FeignClient 'url' attributes <---
        "services.employee.url=http://localhost:${wiremock.server.port}",
        "services.notification.url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class FeignClientsWireMockTest {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private NotificationClient notificationClient;

    @MockBean
    private org.redisson.api.RedissonClient redissonClient;
    @MockBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;
    @MockBean
    private org.springframework.data.redis.connection.ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    // ==========================================
    //        EMPLOYEE CLIENT TESTS
    // ==========================================

    @Test
    void givenValidEmployeeId_whenGetEmployee_thenReturnEmployeeDto() {
        // Arrange: URL fixed to exactly match your Feign Client (@GetMapping("/api/employees/{id}"))
        stubFor(get(urlEqualTo("/api/employees/1"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"id\": 1, \"name\": \"John Doe\", \"email\": \"john.doe@example.com\", \"departmentId\": 101 }")));

        // Act
        EmployeeDto employee = employeeClient.getEmployeeById(1L);

        // Assert
        assertThat(employee).isNotNull();
        assertThat(employee.getId()).isEqualTo(1L);
        assertThat(employee.getName()).isEqualTo("John Doe");
    }

    @Test
    void givenEmployeeServiceDown_whenGetEmployee_thenTriggerFallbackAndThrow503() {
        // Arrange: Simulate Employee Service crashing
        stubFor(get(urlEqualTo("/api/employees/99"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // Act & Assert: EmployeeFallback THROWS an exception to stop the transaction
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            employeeClient.getEmployeeById(99L);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getReason()).contains("Employee Service is currently offline");
    }

    // ==========================================
    //      NOTIFICATION CLIENT TESTS
    // ==========================================

    @Test
    void givenNotificationServiceDown_whenSendNotification_thenFallbackExecutesSilently() {
        // Arrange: Simulate Notification Service crashing
        stubFor(post(urlEqualTo("/api/notifications"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        NotificationDto dto = new NotificationDto();
        // Assuming your NotificationDto has an email field, if not, just ignore this line
        // dto.setRecipientEmail("test@example.com");

        // Act & Assert: NotificationFallback only logs a warning, so it should NOT throw an exception!
        assertDoesNotThrow(() -> {
            notificationClient.sendNotification(dto);
        });
    }
}