package com.example.notificationservice.integrationtest;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.dto.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void testGetStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/notifications/status", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Notification Service is up and running on port 8084!", response.getBody());
    }

    @Test
    void testTriggerNotification() {
        // 1. Prepare DTO
        NotificationDto dto = new NotificationDto();
        dto.setRecipientEmail("bob@example.com");
        dto.setMessage("Your leave request update is ready.");

        // 2. Execute POST request to standard endpoint
        ResponseEntity<Notification> response = restTemplate.postForEntity(
                "/api/notifications",
                dto,
                Notification.class
        );

        // 3. Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bob@example.com", response.getBody().getRecipientEmail());
        assertNotNull(response.getBody().getSentAt());

        // 4. Verify database persistence
        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals("bob@example.com", notifications.get(0).getRecipientEmail());
    }

    @Test
    void testSendWelcomeEmail() {
        // Execute POST with RequestParams
        String url = "/api/notifications/welcome?email=charlie@example.com&name=Charlie";
        ResponseEntity<Notification> response = restTemplate.postForEntity(
                url,
                null,
                Notification.class
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("charlie@example.com", response.getBody().getRecipientEmail());
        assertEquals("Welcome to the company, Charlie! Your employee profile has been successfully created.", response.getBody().getMessage());
    }

    @Test
    void testSendPasswordResetEmail() {
        // Execute POST with RequestParams
        String url = "/api/notifications/password-reset?email=david@example.com&resetToken=ABC-123-XYZ";
        ResponseEntity<Notification> response = restTemplate.postForEntity(
                url,
                null,
                Notification.class
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("david@example.com", response.getBody().getRecipientEmail());
        assertEquals("Password reset requested. Use this token to reset your password: ABC-123-XYZ", response.getBody().getMessage());
    }
}