package com.payguard.reconciliation.service;

import com.payguard.reconciliation.config.KafkaConfig;
import com.payguard.reconciliation.entity.Settlement;
import com.payguard.reconciliation.entity.SettlementRecord;
import com.payguard.reconciliation.repository.SettlementRecordRepository;
import com.payguard.reconciliation.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final SettlementRepository settlementRepository;
    private final SettlementRecordRepository recordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Run reconciliation for a specific date.
     * Called via REST endpoint or scheduled job.
     */
    @Transactional
    public Settlement runReconciliation(LocalDate date) {
        log.info("Starting reconciliation for date: {}", date);

        // Check if already reconciled
        Optional<Settlement> existing = settlementRepository.findBySettlementDate(date);
        if (existing.isPresent() && "COMPLETED".equals(existing.get().getStatus())) {
            log.info("Reconciliation already completed for date: {}", date);
            return existing.get();
        }

        // Get all unmatched records
        List<SettlementRecord> unmatchedRecords = recordRepository.findByMatchedFalse();

        if (unmatchedRecords.isEmpty()) {
            log.info("No records to reconcile for date: {}", date);
            Settlement empty = Settlement.builder()
                    .settlementDate(date)
                    .totalAmount(0L)
                    .transactionCount(0)
                    .discrepancyCount(0)
                    .status("COMPLETED")
                    .details(Map.of("message", "No records to reconcile"))
                    .build();
            return settlementRepository.save(empty);
        }

        // Create settlement record
        Settlement settlement = Settlement.builder()
                .settlementDate(date)
                .totalAmount(0L)
                .transactionCount(0)
                .discrepancyCount(0)
                .status("IN_PROGRESS")
                .build();
        settlement = settlementRepository.save(settlement);

        long totalAmount = 0;
        int matchedCount = 0;
        int discrepancyCount = 0;
        List<Map<String, Object>> discrepancies = new ArrayList<>();

        for (SettlementRecord record : unmatchedRecords) {
            record.setSettlementId(settlement.getId());

            // Simulate Stripe verification:
            // In production, call Stripe API to verify each payment
            if (record.getStripePaymentId() != null && !record.getStripePaymentId().isEmpty()) {
                // Has Stripe ID — simulate successful verification
                record.setStripeStatus("succeeded");
                record.setMatched(true);
                totalAmount += record.getAmount();
                matchedCount++;
            } else {
                // No Stripe ID — discrepancy
                record.setMatched(false);
                record.setDiscrepancyReason("Missing Stripe payment ID");
                discrepancyCount++;
                discrepancies.add(Map.of(
                        "transactionId", record.getTransactionId().toString(),
                        "amount", record.getAmount(),
                        "reason", "Missing Stripe payment ID"
                ));
            }

            recordRepository.save(record);
        }

        // Update settlement summary
        settlement.setTotalAmount(totalAmount);
        settlement.setTransactionCount(matchedCount + discrepancyCount);
        settlement.setDiscrepancyCount(discrepancyCount);
        settlement.setStatus("COMPLETED");
        settlement.setDetails(Map.of(
                "matchedCount", matchedCount,
                "discrepancyCount", discrepancyCount,
                "totalAmountCents", totalAmount,
                "totalAmountFormatted", String.format("$%.2f", totalAmount / 100.0),
                "discrepancies", discrepancies
        ));

        settlement = settlementRepository.save(settlement);

        // Publish event
        publishReconciliationCompleted(settlement);

        log.info("Reconciliation completed for {}: {} transactions, ${}, {} discrepancies",
                date, settlement.getTransactionCount(),
                String.format("%.2f", totalAmount / 100.0),
                discrepancyCount);

        return settlement;
    }

    /**
     * Scheduled daily reconciliation at 2 AM.
     */
    @Scheduled(cron = "${reconciliation.cron}")
    public void scheduledReconciliation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running scheduled reconciliation for {}", yesterday);
        runReconciliation(yesterday);
    }

    /**
     * Get all settlements.
     */
    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAllByOrderBySettlementDateDesc();
    }

    /**
     * Get settlement details with records.
     */
    public Map<String, Object> getSettlementDetails(UUID settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        List<SettlementRecord> records = recordRepository.findBySettlementId(settlementId);
        List<SettlementRecord> discrepancies = recordRepository
                .findBySettlementIdAndMatchedFalse(settlementId);

        return Map.of(
                "settlement", settlement,
                "totalRecords", records.size(),
                "discrepancies", discrepancies
        );
    }

    private void publishReconciliationCompleted(Settlement settlement) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", "evt_" + UUID.randomUUID().toString().substring(0, 8));
            event.put("eventType", "reconciliation.completed");
            event.put("timestamp", Instant.now().toString());
            event.put("source", "reconciliation-service");
            event.put("data", Map.of(
                    "settlementId", settlement.getId().toString(),
                    "settlementDate", settlement.getSettlementDate().toString(),
                    "totalAmount", settlement.getTotalAmount(),
                    "transactionCount", settlement.getTransactionCount(),
                    "discrepancyCount", settlement.getDiscrepancyCount(),
                    "status", settlement.getStatus()
            ));

            kafkaTemplate.send(KafkaConfig.RECONCILIATION_COMPLETED_TOPIC,
                    settlement.getId().toString(), event);
            log.info("Published reconciliation.completed for {}", settlement.getSettlementDate());
        } catch (Exception e) {
            log.error("Failed to publish reconciliation event: {}", e.getMessage());
        }
    }
}