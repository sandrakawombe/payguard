package com.payguard.reconciliation.controller;

import com.payguard.reconciliation.entity.Settlement;
import com.payguard.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    /**
     * Trigger reconciliation for a specific date.
     * In production, this would be admin-only.
     */
    @PostMapping("/run")
    public ResponseEntity<Settlement> runReconciliation(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);
        Settlement result = reconciliationService.runReconciliation(targetDate);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all settlements.
     */
    @GetMapping("/settlements")
    public ResponseEntity<List<Settlement>> getSettlements() {
        return ResponseEntity.ok(reconciliationService.getAllSettlements());
    }

    /**
     * Get settlement details with discrepancies.
     */
    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<Map<String, Object>> getSettlementDetails(
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(reconciliationService.getSettlementDetails(settlementId));
    }

    /**
     * Health and status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        List<Settlement> all = reconciliationService.getAllSettlements();
        Settlement latest = all.isEmpty() ? null : all.get(0);

        return ResponseEntity.ok(Map.of(
                "service", "reconciliation-service",
                "status", "UP",
                "totalSettlements", all.size(),
                "latestSettlementDate", latest != null ? latest.getSettlementDate().toString() : "none",
                "latestStatus", latest != null ? latest.getStatus() : "none"
        ));
    }
}