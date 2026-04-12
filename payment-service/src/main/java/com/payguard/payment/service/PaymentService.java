package com.payguard.payment.service;

import com.payguard.payment.dto.*;
import com.payguard.payment.entity.Refund;
import com.payguard.payment.entity.Transaction;
import com.payguard.payment.entity.Transaction.TransactionStatus;
import com.payguard.payment.exception.*;
import com.payguard.payment.repository.RefundRepository;
import com.payguard.payment.repository.TransactionRepository;
import com.payguard.proto.fraud.FraudScoreResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final FraudEngineClient fraudEngineClient;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Process a payment:
     * 1. Create PENDING transaction
     * 2. Score with Fraud Engine via gRPC
     * 3. If APPROVE → charge via Stripe
     * 4. If BLOCK → reject immediately
     * 5. If REVIEW → hold for manual review
     */
    @Transactional
    public TransactionResponse processPayment(UUID merchantId, ChargeRequest request) {
        // Step 1: Create PENDING transaction
        Transaction txn = Transaction.builder()
                .merchantId(merchantId)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .customerEmail(request.getCustomerEmail().toLowerCase().trim())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .status(TransactionStatus.PENDING)
                .build();

        txn = transactionRepository.save(txn);
        log.info("Created PENDING transaction: {} for merchant: {}", txn.getId(), merchantId);

        eventPublisher.publishPaymentCreated(txn);

        // Step 2: Score with Fraud Engine
        FraudScoreResponse fraudResponse = fraudEngineClient.scoreTransaction(
                txn.getId().toString(),
                merchantId.toString(),
                request.getAmount(),
                request.getCurrency(),
                request.getCustomerEmail(),
                null, // merchantCategory — Fraud Engine can look this up
                null  // country — Fraud Engine can look this up
        );

        txn.setFraudScore(BigDecimal.valueOf(fraudResponse.getFraudScore()));
        txn.setFraudDecision(fraudResponse.getDecision());

        // Step 3: Act on fraud decision
        switch (fraudResponse.getDecision()) {
            case "APPROVE" -> {
                return chargeViaStripe(txn);
            }
            case "BLOCK" -> {
                txn.setStatus(TransactionStatus.BLOCKED);
                txn.setFailureReason("Blocked by fraud detection (score: " + fraudResponse.getFraudScore() + ")");
                transactionRepository.save(txn);
                eventPublisher.publishPaymentFailed(txn, "fraud_blocked");
                log.warn("Payment BLOCKED: {} (fraud score: {})", txn.getId(), fraudResponse.getFraudScore());
                throw new PaymentBlockedException(txn.getId().toString(), fraudResponse.getFraudScore());
            }
            case "REVIEW" -> {
                txn.setStatus(TransactionStatus.REVIEW);
                transactionRepository.save(txn);
                log.info("Payment sent to REVIEW: {} (fraud score: {})", txn.getId(), fraudResponse.getFraudScore());
                return TransactionResponse.fromEntity(txn);
            }
            default -> {
                return chargeViaStripe(txn);
            }
        }
    }

    /**
     * Charge the customer via Stripe.
     */
    private TransactionResponse chargeViaStripe(Transaction txn) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(txn.getAmount())
                    .setCurrency(txn.getCurrency().toLowerCase())
                    .setDescription(txn.getDescription())
                    .putMetadata("payguard_txn_id", txn.getId().toString())
                    .putMetadata("merchant_id", txn.getMerchantId().toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            txn.setStripePaymentId(paymentIntent.getId());
            txn.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(txn);

            eventPublisher.publishPaymentCompleted(txn);
            log.info("Payment COMPLETED: {} (stripe: {})", txn.getId(), paymentIntent.getId());

            return TransactionResponse.fromEntity(txn);

        } catch (StripeException e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailureReason(e.getMessage());
            transactionRepository.save(txn);
            eventPublisher.publishPaymentFailed(txn, e.getCode());
            log.error("Stripe charge failed for transaction {}: {}", txn.getId(), e.getMessage());
            return TransactionResponse.fromEntity(txn);
        }
    }

    /**
     * Process a refund for a completed transaction.
     */
    @Transactional
    public TransactionResponse processRefund(UUID merchantId, UUID transactionId, RefundRequest request) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Verify merchant owns this transaction
        if (!txn.getMerchantId().equals(merchantId)) {
            throw new TransactionNotFoundException(transactionId);
        }

        // Verify transaction is COMPLETED
        if (txn.getStatus() != TransactionStatus.COMPLETED) {
            throw new RuntimeException("Can only refund completed transactions");
        }

        // Verify refund doesn't exceed original amount
        Long totalRefunded = refundRepository.getTotalRefundedAmount(transactionId);
        if (totalRefunded + request.getAmount() > txn.getAmount()) {
            throw new RefundExceedsAmountException();
        }

        try {
            com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.create(
                    com.stripe.param.RefundCreateParams.builder()
                            .setPaymentIntent(txn.getStripePaymentId())
                            .setAmount(request.getAmount())
                            .build()
            );

            Refund refund = Refund.builder()
                    .transactionId(transactionId)
                    .stripeRefundId(stripeRefund.getId())
                    .amount(request.getAmount())
                    .reason(request.getReason())
                    .status("COMPLETED")
                    .build();

            refundRepository.save(refund);
            log.info("Refund COMPLETED: {} for transaction {} (amount: {})",
                    refund.getId(), transactionId, request.getAmount());

            return TransactionResponse.fromEntity(txn);

        } catch (StripeException e) {
            log.error("Stripe refund failed for transaction {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID merchantId, UUID transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!txn.getMerchantId().equals(merchantId)) {
            throw new TransactionNotFoundException(transactionId);
        }

        return TransactionResponse.fromEntity(txn);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Handle Stripe webhook — idempotent processing.
     */
    @Transactional
    public void handleStripeWebhook(String eventType, String paymentIntentId, String status) {
        if (!"payment_intent.succeeded".equals(eventType)
                && !"payment_intent.payment_failed".equals(eventType)) {
            log.debug("Ignoring Stripe webhook event type: {}", eventType);
            return;
        }

        transactionRepository.findByStripePaymentId(paymentIntentId).ifPresent(txn -> {
            if (txn.getStatus() == TransactionStatus.COMPLETED
                    || txn.getStatus() == TransactionStatus.FAILED) {
                log.info("Webhook already processed for stripe payment: {} (idempotent skip)", paymentIntentId);
                return;
            }

            if ("payment_intent.succeeded".equals(eventType)) {
                txn.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(txn);
                eventPublisher.publishPaymentCompleted(txn);
                log.info("Webhook confirmed payment success: {}", paymentIntentId);
            } else {
                txn.setStatus(TransactionStatus.FAILED);
                txn.setFailureReason("Payment failed (webhook)");
                transactionRepository.save(txn);
                eventPublisher.publishPaymentFailed(txn, "stripe_webhook_failed");
                log.info("Webhook confirmed payment failure: {}", paymentIntentId);
            }
        });
    }
}