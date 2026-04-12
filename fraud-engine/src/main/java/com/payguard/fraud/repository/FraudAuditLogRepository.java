package com.payguard.fraud.repository;

import com.payguard.fraud.entity.FraudAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAuditLogRepository extends JpaRepository<FraudAuditLog, UUID> {

    List<FraudAuditLog> findByTransactionId(UUID transactionId);

    List<FraudAuditLog> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}