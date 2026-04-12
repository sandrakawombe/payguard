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
public class PaymentEventConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(topics = "payment.completed", groupId = "notification-service")
    public void handlePaymentCompleted(Map<String, Object> event) {
        try {
            log.info("Received payment.completed event: {}", event.get("eventId"));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            String merchantId = (String) data.get("merchantId");
            String customerEmail = (String) data.get("customerEmail");
            Object amountObj = data.get("amount");
            long amount = amountObj instanceof Number ? ((Number) amountObj).longValue() : 0L;
            String currency = (String) data.getOrDefault("currency", "USD");
            String transactionId = (String) data.get("transactionId");

            String subject = "PayGuard — Payment Receipt";
            String body = String.format(
                    "Hello,\n\n"
                    + "Your payment has been processed successfully.\n\n"
                    + "--- Payment Receipt ---\n"
                    + "Transaction ID: %s\n"
                    + "Amount: %s %.2f\n"
                    + "Status: COMPLETED\n"
                    + "-----------------------\n\n"
                    + "Thank you for using PayGuard.\n\n"
                    + "— PayGuard",
                    transactionId, currency.toUpperCase(), amount / 100.0
            );

            notificationProcessor.processNotification(
                    UUID.fromString(merchantId), customerEmail,
                    "PAYMENT_RECEIPT", subject, body, data
            );
        } catch (Exception e) {
            log.error("Failed to process payment.completed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-service")
    public void handlePaymentFailed(Map<String, Object> event) {
        try {
            log.info("Received payment.failed event: {}", event.get("eventId"));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            String merchantId = (String) data.get("merchantId");
            String customerEmail = (String) data.get("customerEmail");
            String transactionId = (String) data.get("transactionId");
            String failureReason = (String) data.getOrDefault("failureReason", "Unknown");

            String subject = "PayGuard — Payment Failed";
            String body = String.format(
                    "Hello,\n\nYour payment (Transaction: %s) was not processed.\n"
                    + "Reason: %s\n\nPlease try again.\n\n— PayGuard",
                    transactionId, failureReason
            );

            notificationProcessor.processNotification(
                    UUID.fromString(merchantId), customerEmail,
                    "PAYMENT_FAILED", subject, body, data
            );
        } catch (Exception e) {
            log.error("Failed to process payment.failed: {}", e.getMessage(), e);
        }
    }
}