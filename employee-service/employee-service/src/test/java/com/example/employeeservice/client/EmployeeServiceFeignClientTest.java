package com.example.employeeservice.client; // Adjust if your tests live in a different folder

import com.example.employeeservice.dto.DepartmentDto;
import com.example.employeeservice.dto.NotificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        // Perfectly targets the exact properties from your @FeignClient annotations
        "services.department.url=http://localhost:${wiremock.server.port}",
        "services.notification.url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class EmployeeServiceFeignClientsTest {

    @Autowired
    private DepartmentClient departmentClient;

    @Autowired
    private NotificationClient notificationClient;

    // ==========================================
    //        DEPARTMENT CLIENT TESTS
    // ==========================================

    @Test
    void givenValidDepartmentId_whenGetDepartment_thenReturnDepartmentDto() {
        // Arrange: Happy path using exact JSON keys from your DepartmentDto
        stubFor(get(urlEqualTo("/api/departments/1"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"id\": 1, \"departmentName\": \"Engineering\", \"departmentCode\": \"ENG-01\" }")));

        // Act
        DepartmentDto department = departmentClient.getDepartmentById(1L);

        // Assert
        assertThat(department).isNotNull();
        assertThat(department.getId()).isEqualTo(1L);
        assertThat(department.getDepartmentName()).isEqualTo("Engineering");
        assertThat(department.getDepartmentCode()).isEqualTo("ENG-01");
    }

    @Test
    void givenDepartmentServiceDown_whenGetDepartment_thenTriggerFallbackAndReturnDummyObject() {
        // Arrange: Simulate Department Service crashing
        stubFor(get(urlEqualTo("/api/departments/99"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // Act: Because of your fallback, this will NOT throw an exception!
        DepartmentDto fallbackDepartment = departmentClient.getDepartmentById(99L);

        // Assert: Verify it returns the dummy object created in your Fallback class
        assertThat(fallbackDepartment).isNotNull();
        assertThat(fallbackDepartment.getId()).isEqualTo(99L);
        // Since your fallback only sets the ID, the name and code should be null
        assertThat(fallbackDepartment.getDepartmentName()).isNull();
        assertThat(fallbackDepartment.getDepartmentCode()).isNull();
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
        dto.setRecipientEmail("employee.onboarding@example.com");
        dto.setMessage("Welcome to the team!");

        // Act & Assert: Should degrade gracefully without throwing an exception
        // (IntelliJ's suggested expression lambda format applied here)
        assertDoesNotThrow(() -> notificationClient.sendNotification(dto));
    }
}