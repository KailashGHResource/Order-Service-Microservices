package com.example.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDto {
    private Long id;
    private String recipientEmail;
    private String message;
    private LocalDateTime sentAt;
}