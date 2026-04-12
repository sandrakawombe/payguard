package com.payguard.reconciliation.repository;

import com.payguard.reconciliation.entity.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID> {

    List<SettlementRecord> findByMatchedFalse();

    List<SettlementRecord> findBySettlementId(UUID settlementId);

    List<SettlementRecord> findBySettlementIdAndMatchedFalse(UUID settlementId);
}