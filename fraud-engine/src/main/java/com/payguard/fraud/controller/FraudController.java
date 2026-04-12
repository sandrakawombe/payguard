package com.payguard.fraud.controller;

import com.payguard.fraud.dto.FraudScoreResult;
import com.payguard.fraud.entity.FraudAuditLog;
import com.payguard.fraud.repository.FraudAuditLogRepository;
import com.payguard.fraud.service.FraudScoringOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudScoringOrchestrator scoringOrchestrator;
    private final FraudAuditLogRepository auditLogRepository;

    /**
     * REST endpoint for fraud scoring — used for testing/debugging.
     * In production, Payment Service uses gRPC instead.
     */
    @PostMapping("/score")
    public ResponseEntity<FraudScoreResult> scoreTransaction(@RequestBody Map<String, Object> request) {
        FraudScoreResult result = scoringOrchestrator.scoreTransaction(
                request.getOrDefault("transactionId", UUID.randomUUID().toString()).toString(),
                request.getOrDefault("merchantId", UUID.randomUUID().toString()).toString(),
                Long.parseLong(request.getOrDefault("amountCents", 0).toString()),
                request.getOrDefault("currency", "USD").toString(),
                request.getOrDefault("customerEmail", "").toString(),
                request.getOrDefault("merchantCategory", "retail").toString(),
                request.getOrDefault("country", "USA").toString(),
                Long.parseLong(request.getOrDefault("timestamp", System.currentTimeMillis() / 1000).toString())
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Get fraud audit log for a specific transaction.
     */
    @GetMapping("/audit/{transactionId}")
    public ResponseEntity<List<FraudAuditLog>> getAuditLog(@PathVariable UUID transactionId) {
        List<FraudAuditLog> logs = auditLogRepository.findByTransactionId(transactionId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Model health and version info.
     */
    @GetMapping("/model/health")
    public ResponseEntity<Map<String, Object>> modelHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "scoringEngine", "rule-based",
                "modelVersion", "rules-v1.0.0",
                "mlModelStatus", "NOT_DEPLOYED",
                "note", "ONNX ML model will be deployed in Phase 3"
        ));
    }
}