package com.payguard.reconciliation;

import com.payguard.reconciliation.entity.Settlement;
import com.payguard.reconciliation.entity.SettlementRecord;
import com.payguard.reconciliation.repository.SettlementRepository;
import com.payguard.reconciliation.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementRecordRepository recordRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void shouldReconcileMatchedRecords() {
        LocalDate today = LocalDate.now();
        when(settlementRepository.findBySettlementDate(today)).thenReturn(Optional.empty());

        SettlementRecord matched = SettlementRecord.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .amount(5000L)
                .currency("USD")
                .stripePaymentId("pi_test123")
                .payguardStatus("COMPLETED")
                .matched(false)
                .build();

        when(recordRepository.findByMatchedFalse()).thenReturn(List.of(matched));

        Settlement savedSettlement = Settlement.builder()
                .id(UUID.randomUUID())
                .settlementDate(today)
                .totalAmount(0L)
                .transactionCount(0)
                .discrepancyCount(0)
                .status("IN_PROGRESS")
                .build();
        when(settlementRepository.save(any(Settlement.class))).thenReturn(savedSettlement);
        when(recordRepository.save(any(SettlementRecord.class))).thenReturn(matched);

        Settlement result = reconciliationService.runReconciliation(today);

        verify(recordRepository).save(any(SettlementRecord.class));
        verify(settlementRepository, atLeast(2)).save(any(Settlement.class));
    }

    @Test
    void shouldFlagDiscrepancies() {
        LocalDate today = LocalDate.now();
        when(settlementRepository.findBySettlementDate(today)).thenReturn(Optional.empty());

        SettlementRecord noStripe = SettlementRecord.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .amount(10000L)
                .currency("USD")
                .stripePaymentId(null)
                .payguardStatus("COMPLETED")
                .matched(false)
                .build();

        when(recordRepository.findByMatchedFalse()).thenReturn(List.of(noStripe));

        Settlement savedSettlement = Settlement.builder()
                .id(UUID.randomUUID())
                .settlementDate(today)
                .totalAmount(0L)
                .transactionCount(0)
                .discrepancyCount(0)
                .status("IN_PROGRESS")
                .build();
        when(settlementRepository.save(any(Settlement.class))).thenReturn(savedSettlement);
        when(recordRepository.save(any(SettlementRecord.class))).thenReturn(noStripe);

        Settlement result = reconciliationService.runReconciliation(today);

        verify(recordRepository).save(argThat(record ->
                !record.getMatched() && "Missing Stripe payment ID".equals(record.getDiscrepancyReason())
        ));
    }

    @Test
    void shouldSkipAlreadyReconciledDate() {
        LocalDate today = LocalDate.now();
        Settlement existing = Settlement.builder()
                .id(UUID.randomUUID())
                .settlementDate(today)
                .status("COMPLETED")
                .build();

        when(settlementRepository.findBySettlementDate(today)).thenReturn(Optional.of(existing));

        Settlement result = reconciliationService.runReconciliation(today);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(recordRepository, never()).findByMatchedFalse();
    }
}