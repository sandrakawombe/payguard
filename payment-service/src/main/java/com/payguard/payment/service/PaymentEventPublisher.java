package com.payguard.payment.service;

import com.payguard.payment.config.KafkaConfig;
import com.payguard.payment.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCreated(Transaction txn) {
        publish(KafkaConfig.PAYMENT_CREATED_TOPIC, "payment.created", txn, null);
    }

    public void publishPaymentCompleted(Transaction txn) {
        publish(KafkaConfig.PAYMENT_COMPLETED_TOPIC, "payment.completed", txn, null);
    }

    public void publishPaymentFailed(Transaction txn, String failureReason) {
        publish(KafkaConfig.PAYMENT_FAILED_TOPIC, "payment.failed", txn, failureReason);
    }

    private void publish(String topic, String eventType, Transaction txn, String failureReason) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", "evt_" + UUID.randomUUID().toString().substring(0, 8));
            event.put("eventType", eventType);
            event.put("timestamp", Instant.now().toString());
            event.put("source", "payment-service");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionId", txn.getId().toString());
            data.put("merchantId", txn.getMerchantId().toString());
            data.put("amount", txn.getAmount());
            data.put("currency", txn.getCurrency());
            data.put("customerEmail", txn.getCustomerEmail());
            data.put("status", txn.getStatus().name());

            if (txn.getStripePaymentId() != null) {
                data.put("stripePaymentId", txn.getStripePaymentId());
            }
            if (txn.getFraudScore() != null) {
                data.put("fraudScore", txn.getFraudScore());
                data.put("fraudDecision", txn.getFraudDecision());
            }
            if (failureReason != null) {
                data.put("failureReason", failureReason);
            }

            event.put("data", data);

            kafkaTemplate.send(topic, txn.getMerchantId().toString(), event);
            log.info("Published {} for transaction {}", eventType, txn.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} for transaction {}: {}",
                    eventType, txn.getId(), e.getMessage());
        }
    }
}