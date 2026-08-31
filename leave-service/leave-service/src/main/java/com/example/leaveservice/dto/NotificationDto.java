package com.example.leaveservice.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private String recipientEmail;
    private String message;
}