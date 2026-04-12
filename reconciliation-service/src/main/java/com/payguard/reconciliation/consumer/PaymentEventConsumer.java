package com.payguard.reconciliation.consumer;

import com.payguard.reconciliation.entity.SettlementRecord;
import com.payguard.reconciliation.repository.SettlementRecordRepository;
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

    private final SettlementRecordRepository recordRepository;

    @KafkaListener(topics = "payment.completed", groupId = "reconciliation-service")
    public void handlePaymentCompleted(Map<String, Object> event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            String transactionId = (String) data.get("transactionId");
            String merchantId = (String) data.get("merchantId");
            Object amountObj = data.get("amount");
            long amount = amountObj instanceof Number ? ((Number) amountObj).longValue() : 0L;
            String currency = (String) data.getOrDefault("currency", "USD");
            String stripePaymentId = (String) data.get("stripePaymentId");

            SettlementRecord record = SettlementRecord.builder()
                    .transactionId(UUID.fromString(transactionId))
                    .merchantId(UUID.fromString(merchantId))
                    .amount(amount)
                    .currency(currency)
                    .stripePaymentId(stripePaymentId)
                    .payguardStatus("COMPLETED")
                    .matched(false)
                    .build();

            recordRepository.save(record);
            log.info("Recorded payment for reconciliation: {} (amount: {})", transactionId, amount);

        } catch (Exception e) {
            log.error("Failed to record payment event: {}", e.getMessage(), e);
        }
    }
}