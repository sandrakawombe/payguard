package com.payguard.payment.exception;

public class RefundExceedsAmountException extends RuntimeException {
    public RefundExceedsAmountException() {
        super("Total refund amount exceeds the original transaction amount");
    }
}