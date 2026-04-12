package com.payguard.payment.controller;

import com.payguard.payment.dto.*;
import com.payguard.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<TransactionResponse> charge(
            @RequestHeader("X-User-Id") UUID merchantId,
            @Valid @RequestBody ChargeRequest request) {
        TransactionResponse response = paymentService.processPayment(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<TransactionResponse> refund(
            @RequestHeader("X-User-Id") UUID merchantId,
            @PathVariable UUID transactionId,
            @Valid @RequestBody RefundRequest request) {
        TransactionResponse response = paymentService.processRefund(merchantId, transactionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @RequestHeader("X-User-Id") UUID merchantId,
            @PathVariable UUID transactionId) {
        TransactionResponse response = paymentService.getTransaction(merchantId, transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @RequestHeader("X-User-Id") UUID merchantId) {
        List<TransactionResponse> response = paymentService.getTransactions(merchantId);
        return ResponseEntity.ok(response);
    }
}