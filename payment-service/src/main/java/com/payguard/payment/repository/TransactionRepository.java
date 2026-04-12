package com.payguard.payment.repository;

import com.payguard.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByStripePaymentId(String stripePaymentId);

    List<Transaction> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    boolean existsByStripePaymentId(String stripePaymentId);
}