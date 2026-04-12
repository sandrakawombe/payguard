package com.payguard.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "settlement_id")
    private UUID settlementId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "stripe_payment_id")
    private String stripePaymentId;

    @Column(name = "payguard_status", nullable = false, length = 20)
    private String payguardStatus;

    @Column(name = "stripe_status", length = 20)
    private String stripeStatus;

    @Column(nullable = false)
    private Boolean matched;

    @Column(name = "discrepancy_reason")
    private String discrepancyReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}