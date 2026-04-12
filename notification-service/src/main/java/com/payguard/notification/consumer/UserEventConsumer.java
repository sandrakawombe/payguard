package com.payguard.notification.consumer;

import com.payguard.notification.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void handleUserRegistered(Map<String, Object> event) {
        try {
            log.info("Received user.registered event: {}", event.get("eventId"));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            String userId = (String) data.get("userId");
            String email = (String) data.get("email");
            String merchantName = (String) data.getOrDefault("merchantName", "Merchant");

            String subject = "Welcome to PayGuard!";
            String body = String.format(
                    "Hello %s,\n\n"
                    + "Welcome to PayGuard! Your merchant account is ready.\n\n"
                    + "You can now:\n"
                    + "- Process payments via our API\n"
                    + "- Monitor transactions in real-time\n"
                    + "- View fraud scores and analytics\n\n"
                    + "— The PayGuard Team",
                    merchantName
            );

            notificationProcessor.processNotification(
                    UUID.fromString(userId), email,
                    "WELCOME", subject, body, data
            );
        } catch (Exception e) {
            log.error("Failed to process user.registered: {}", e.getMessage(), e);
        }
    }
}