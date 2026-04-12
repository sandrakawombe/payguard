package com.payguard.fraud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFeatures {

    private long amountCents;
    private int hourOfDay;
    private int dayOfWeek;
    private String merchantCategory;
    private long userAvgAmountCents;
    private double amountDeviation;
    private int txVelocity1h;
    private int txVelocity24h;
    private boolean newMerchant;
    private boolean countryMismatch;
    private int failedAttempts24h;
}