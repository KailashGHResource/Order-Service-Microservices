package com.example.notificationservice.mapper;

import com.example.notificationservice.dto.NotificationResponseDto;
import com.example.notificationservice.entity.Notification; // Update import if you moved the entity
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDto toDto(Notification entity) {
        if (entity == null) return null;

        return NotificationResponseDto.builder()
                .id(entity.getId())
                .recipientEmail(entity.getRecipientEmail())
                .message(entity.getMessage())
                .sentAt(entity.getSentAt())
                .build();
    }
}