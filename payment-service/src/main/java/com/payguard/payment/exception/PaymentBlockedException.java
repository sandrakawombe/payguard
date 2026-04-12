package com.payguard.payment.exception;

public class PaymentBlockedException extends RuntimeException {
    public PaymentBlockedException(String transactionId, double score) {
        super("Payment blocked by fraud detection. Transaction: " + transactionId + ", score: " + score);
    }
}