package com.example.notificationservice.controller;

import com.example.notificationservice.application.NotificationService;
import com.example.notificationservice.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications") // Adopting the V1 versioning standard
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/status")
    public String getStatus() {
        log.info("Received request to check Notification Service status");
        return "Notification Service V1 is up and running on port 8084!";
    }

    // 1. Audit Log: View all sent notifications safely via DTOs
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    // 2. Password Reset Email Trigger
    @PostMapping("/password-reset")
    public ResponseEntity<NotificationResponseDto> sendPasswordResetEmail(
            @RequestParam String email,
            @RequestParam String resetToken) {

        NotificationResponseDto responseDto = notificationService.sendPasswordResetEmail(email, resetToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}