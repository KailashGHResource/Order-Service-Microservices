package com.example.notificationservice.application;

import com.example.notificationservice.dto.NotificationResponseDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public List<NotificationResponseDto> getAllNotifications() {
        log.info("Fetching all sent notifications from database");
        return notificationRepository.findAll().stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    public NotificationResponseDto sendPasswordResetEmail(String email, String resetToken) {
        log.info("Processing password reset email for: {}", email);

        Notification notification = new Notification();
        notification.setRecipientEmail(email);
        notification.setMessage("Password reset requested. Use this token to reset your password: " + resetToken);
        notification.setSentAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Password reset notification saved with ID: {}", savedNotification.getId());

        return notificationMapper.toDto(savedNotification);
    }
}