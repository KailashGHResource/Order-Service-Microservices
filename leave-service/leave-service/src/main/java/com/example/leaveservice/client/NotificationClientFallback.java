package com.example.leaveservice.client;

import com.example.leaveservice.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        // Controlled response: Log warning instead of failing the primary leave action
        log.warn("Notification Service is down or unreachable. Could not send notification for recipient: {}. Fallback executed.",
                notificationDto != null ? notificationDto.getRecipientEmail() : "UNKNOWN");

        // Optional: Save to dead-letter queue / database for retry later if required
    }
}