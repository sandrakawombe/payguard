package com.payguard.fraud.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fraud_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "fraud_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal fraudScore;

    @Column(nullable = false, length = 20)
    private String decision;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> featuresJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_factors", nullable = false, columnDefinition = "jsonb")
    private List<String> contributingFactors;

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs;

    @Column(name = "fallback_used", nullable = false)
    private Boolean fallbackUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}