package com.payguard.fraud.service;

import com.payguard.fraud.config.KafkaConfig;
import com.payguard.fraud.dto.FraudScoreResult;
import com.payguard.fraud.dto.TransactionFeatures;
import com.payguard.fraud.entity.FraudAuditLog;
import com.payguard.fraud.repository.FraudAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudScoringOrchestrator {

    private final RuleBasedScorer ruleBasedScorer;
    private final VelocityTracker velocityTracker;
    private final ScoreCache scoreCache;
    private final FraudAuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Score a transaction end-to-end:
     * 1. Check cache
     * 2. Gather velocity features
     * 3. Run scoring
     * 4. Cache result
     * 5. Update velocity counters
     * 6. Save audit log
     * 7. Publish Kafka events
     */
    public FraudScoreResult scoreTransaction(String transactionId, String merchantId,
                                              long amountCents, String currency,
                                              String customerEmail, String merchantCategory,
                                              String country, long timestamp) {
        long startTime = System.currentTimeMillis();

        // Step 1: Check cache
        Optional<FraudScoreResult> cached = scoreCache.get(merchantId, amountCents, customerEmail);
        if (cached.isPresent()) {
            log.info("Cache hit for transaction {}", transactionId);
            FraudScoreResult cachedResult = cached.get();
            cachedResult.setTransactionId(transactionId);
            return cachedResult;
        }

        // Step 2: Gather velocity features from Redis
        int velocity1h = velocityTracker.getCount(customerEmail, "1h");
        int velocity24h = velocityTracker.getCount(customerEmail, "24h");
        int failedAttempts = velocityTracker.getFailedAttempts(customerEmail);

        // Calculate time features
        Instant instant = Instant.ofEpochSecond(timestamp);
        ZonedDateTime zdt = instant.atZone(ZoneId.of("UTC"));
        int hourOfDay = zdt.getHour();
        int dayOfWeek = zdt.getDayOfWeek().getValue();

        // Calculate amount deviation (use average of 5000 cents = $50 as default for new users)
        long avgAmount = 5000L; // TODO: Pull from historical data or user profile
        double deviation = avgAmount > 0 ? (double) amountCents / avgAmount : 1.0;

        // Step 3: Assemble features
        TransactionFeatures features = TransactionFeatures.builder()
                .amountCents(amountCents)
                .hourOfDay(hourOfDay)
                .dayOfWeek(dayOfWeek)
                .merchantCategory(merchantCategory != null ? merchantCategory : "unknown")
                .userAvgAmountCents(avgAmount)
                .amountDeviation(deviation)
                .txVelocity1h(velocity1h)
                .txVelocity24h(velocity24h)
                .newMerchant(false) // TODO: determine from transaction history
                .countryMismatch(false) // TODO: compare card country vs merchant country
                .failedAttempts24h(failedAttempts)
                .build();

        // Step 4: Run scoring
        FraudScoreResult result = ruleBasedScorer.score(transactionId, features);

        // Update latency to include full orchestration time
        int totalLatency = (int) (System.currentTimeMillis() - startTime);
        result.setLatencyMs(totalLatency);

        // Step 5: Cache the result
        scoreCache.put(merchantId, amountCents, customerEmail, result);

        // Step 6: Update velocity counters
        velocityTracker.incrementAndGet(customerEmail, "1h", Duration.ofHours(1));
        velocityTracker.incrementAndGet(customerEmail, "24h", Duration.ofHours(24));

        // Step 7: Save audit log
        saveAuditLog(transactionId, merchantId, result);

        // Step 8: Publish Kafka events
        publishEvents(transactionId, merchantId, amountCents, result);

        log.info("Scored transaction {} → {} (score={}, latency={}ms)",
                transactionId, result.getDecision(), result.getFraudScore(), totalLatency);

        return result;
    }

    private void saveAuditLog(String transactionId, String merchantId, FraudScoreResult result) {
        try {
            FraudAuditLog auditLog = FraudAuditLog.builder()
                    .transactionId(UUID.fromString(transactionId))
                    .merchantId(UUID.fromString(merchantId))
                    .fraudScore(BigDecimal.valueOf(result.getFraudScore()))
                    .decision(result.getDecision())
                    .modelVersion(result.getModelVersion())
                    .featuresJson(result.getFeatures())
                    .contributingFactors(result.getContributingFactors())
                    .latencyMs(result.getLatencyMs())
                    .fallbackUsed(result.isFallbackUsed())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Audit log failure should NOT block the scoring response
            log.error("Failed to save audit log for transaction {}: {}", transactionId, e.getMessage());
        }
    }

    private void publishEvents(String transactionId, String merchantId,
                                long amountCents, FraudScoreResult result) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", "evt_" + UUID.randomUUID().toString().substring(0, 8));
            event.put("eventType", "fraud.scored");
            event.put("timestamp", Instant.now().toString());
            event.put("source", "fraud-engine");
            event.put("data", Map.of(
                    "transactionId", transactionId,
                    "merchantId", merchantId,
                    "fraudScore", result.getFraudScore(),
                    "decision", result.getDecision(),
                    "modelVersion", result.getModelVersion(),
                    "latencyMs", result.getLatencyMs(),
                    "fallbackUsed", result.isFallbackUsed()
            ));

            kafkaTemplate.send(KafkaConfig.FRAUD_SCORED_TOPIC, transactionId, event);

            // If high risk, also publish alert
            if ("BLOCK".equals(result.getDecision())) {
                Map<String, Object> alertEvent = new LinkedHashMap<>();
                alertEvent.put("eventId", "evt_" + UUID.randomUUID().toString().substring(0, 8));
                alertEvent.put("eventType", "fraud.alert.high");
                alertEvent.put("timestamp", Instant.now().toString());
                alertEvent.put("source", "fraud-engine");
                alertEvent.put("data", Map.of(
                        "transactionId", transactionId,
                        "merchantId", merchantId,
                        "fraudScore", result.getFraudScore(),
                        "decision", result.getDecision(),
                        "amountCents", amountCents,
                        "contributingFactors", result.getContributingFactors()
                ));

                kafkaTemplate.send(KafkaConfig.FRAUD_ALERT_HIGH_TOPIC, merchantId, alertEvent);
                log.warn("HIGH FRAUD ALERT: transaction={}, score={}", transactionId, result.getFraudScore());
            }
        } catch (Exception e) {
            // Kafka failure should NOT block the scoring response
            log.error("Failed to publish fraud event for transaction {}: {}", transactionId, e.getMessage());
        }
    }
}