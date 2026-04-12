package com.payguard.payment.service;

import com.payguard.payment.dto.ChargeRequest;
import com.payguard.payment.dto.TransactionResponse;
import com.payguard.payment.entity.Transaction;
import com.payguard.payment.exception.PaymentBlockedException;
import com.payguard.payment.repository.RefundRepository;
import com.payguard.payment.repository.TransactionRepository;
import com.payguard.proto.fraud.FraudScoreResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private FraudEngineClient fraudEngineClient;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldBlockPaymentWhenFraudScoreIsHigh() {
        UUID merchantId = UUID.randomUUID();
        ChargeRequest request = ChargeRequest.builder()
                .amount(500000)
                .currency("USD")
                .customerEmail("suspicious@test.com")
                .build();

        Transaction savedTxn = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(500000L)
                .currency("USD")
                .customerEmail("suspicious@test.com")
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        FraudScoreResponse fraudResponse = FraudScoreResponse.newBuilder()
                .setTransactionId(savedTxn.getId().toString())
                .setFraudScore(0.92)
                .setDecision("BLOCK")
                .addContributingFactors("extreme_amount")
                .setModelVersion("rules-v1.0.0")
                .setLatencyMs(5)
                .setFallbackUsed(false)
                .build();

        when(fraudEngineClient.scoreTransaction(any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(fraudResponse);

        assertThatThrownBy(() -> paymentService.processPayment(merchantId, request))
                .isInstanceOf(PaymentBlockedException.class);

        verify(eventPublisher).publishPaymentFailed(any(), eq("fraud_blocked"));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void shouldSendToReviewWhenFraudScoreIsMedium() {
        UUID merchantId = UUID.randomUUID();
        ChargeRequest request = ChargeRequest.builder()
                .amount(25000)
                .currency("USD")
                .customerEmail("review@test.com")
                .build();

        Transaction savedTxn = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(25000L)
                .currency("USD")
                .customerEmail("review@test.com")
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        FraudScoreResponse fraudResponse = FraudScoreResponse.newBuilder()
                .setTransactionId(savedTxn.getId().toString())
                .setFraudScore(0.55)
                .setDecision("REVIEW")
                .setModelVersion("rules-v1.0.0")
                .setLatencyMs(5)
                .setFallbackUsed(false)
                .build();

        when(fraudEngineClient.scoreTransaction(any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(fraudResponse);

        TransactionResponse response = paymentService.processPayment(merchantId, request);

        assertThat(response.getStatus()).isEqualTo("REVIEW");
    }
}