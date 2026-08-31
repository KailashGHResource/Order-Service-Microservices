package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientEmail;

    @Column(length = 1000) // Ensures long email bodies don't cause database errors
    private String message;

    private LocalDateTime sentAt; // Automatically logs exactly when the email was "sent"
}