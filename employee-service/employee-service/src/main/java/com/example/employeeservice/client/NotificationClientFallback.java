package com.example.employeeservice.client;

import com.example.employeeservice.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        // We assume NotificationDto has getRecipientEmail() based on your previous DTO
        log.warn("Notification Service is down or unreachable. Could not send notification for: {}. Fallback executed.",
                notificationDto != null ? notificationDto.getRecipientEmail() : "UNKNOWN");

        // Since the return type is void, we just log and finish silently.
        // This ensures the primary transaction (e.g., saving an employee) succeeds even if the notification fails.
    }
}