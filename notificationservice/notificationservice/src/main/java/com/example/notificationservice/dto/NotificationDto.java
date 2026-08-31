package com.example.notificationservice.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private String recipientEmail;
    private String message;
}