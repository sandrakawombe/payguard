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
public class FraudAlertConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(topics = "fraud.alert.high", groupId = "notification-service")
    public void handleFraudAlert(Map<String, Object> event) {
        try {
            log.warn("Received fraud.alert.high event: {}", event.get("eventId"));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            String merchantId = (String) data.get("merchantId");
            String transactionId = (String) data.get("transactionId");
            Object scoreObj = data.get("fraudScore");
            double fraudScore = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
            Object amountObj = data.get("amountCents");
            long amountCents = amountObj instanceof Number ? ((Number) amountObj).longValue() : 0L;

            String subject = "PayGuard ALERT — Suspicious Transaction Blocked";
            String body = String.format(
                    "FRAUD ALERT\n\n"
                    + "A suspicious transaction has been blocked.\n\n"
                    + "Transaction ID: %s\n"
                    + "Amount: $%.2f\n"
                    + "Fraud Score: %.1f%%\n"
                    + "Decision: BLOCKED\n\n"
                    + "— PayGuard Security Team",
                    transactionId, amountCents / 100.0, fraudScore * 100
            );

            notificationProcessor.processNotification(
                    UUID.fromString(merchantId), "admin@payguard.com",
                    "FRAUD_ALERT", subject, body, data
            );
        } catch (Exception e) {
            log.error("Failed to process fraud.alert.high: {}", e.getMessage(), e);
        }
    }
}