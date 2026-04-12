package com.payguard.payment.dto;

import com.payguard.payment.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID merchantId;
    private long amount;
    private String currency;
    private String status;
    private BigDecimal fraudScore;
    private String fraudDecision;
    private String customerEmail;
    private String description;
    private Map<String, Object> metadata;
    private String failureReason;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .merchantId(txn.getMerchantId())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .status(txn.getStatus().name())
                .fraudScore(txn.getFraudScore())
                .fraudDecision(txn.getFraudDecision())
                .customerEmail(txn.getCustomerEmail())
                .description(txn.getDescription())
                .metadata(txn.getMetadata())
                .failureReason(txn.getFailureReason())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}