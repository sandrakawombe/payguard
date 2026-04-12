package com.payguard.payment.service;

import com.payguard.proto.fraud.FraudScoreRequest;
import com.payguard.proto.fraud.FraudScoreResponse;
import com.payguard.proto.fraud.FraudScoringServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FraudEngineClient {

    @GrpcClient("fraud-engine")
    private FraudScoringServiceGrpc.FraudScoringServiceBlockingStub fraudStub;

    @Value("${fraud.fallback.max-amount-cents}")
    private long fallbackMaxAmount;

    /**
     * Score a transaction via gRPC to Fraud Engine.
     * Falls back to simple rules if Fraud Engine is unavailable.
     */
    public FraudScoreResponse scoreTransaction(String transactionId, String merchantId,
                                                long amountCents, String currency,
                                                String customerEmail, String merchantCategory,
                                                String country) {
        try {
            FraudScoreRequest request = FraudScoreRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .setMerchantId(merchantId)
                    .setAmountCents(amountCents)
                    .setCurrency(currency)
                    .setCustomerEmail(customerEmail)
                    .setMerchantCategory(merchantCategory != null ? merchantCategory : "unknown")
                    .setCountry(country != null ? country : "US")
                    .setTimestamp(System.currentTimeMillis() / 1000)
                    .build();

            FraudScoreResponse response = fraudStub.scoreTransaction(request);
            log.info("Fraud Engine scored transaction {}: {} (score={})",
                    transactionId, response.getDecision(), response.getFraudScore());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("Fraud Engine gRPC call failed for transaction {}: {}. Using fallback.",
                    transactionId, e.getStatus());
            return fallbackScore(transactionId, amountCents);
        } catch (Exception e) {
            log.error("Unexpected error calling Fraud Engine for transaction {}: {}. Using fallback.",
                    transactionId, e.getMessage());
            return fallbackScore(transactionId, amountCents);
        }
    }

    /**
     * Simple fallback when Fraud Engine is unavailable.
     * Approve small transactions, block large ones.
     */
    private FraudScoreResponse fallbackScore(String transactionId, long amountCents) {
        boolean isSmallAmount = amountCents <= fallbackMaxAmount;

        return FraudScoreResponse.newBuilder()
                .setTransactionId(transactionId)
                .setFraudScore(isSmallAmount ? 0.1 : 0.8)
                .setDecision(isSmallAmount ? "APPROVE" : "BLOCK")
                .addContributingFactors(isSmallAmount ? "fallback_small_amount" : "fallback_large_amount")
                .setModelVersion("fallback-v1.0.0")
                .setLatencyMs(0)
                .setFallbackUsed(true)
                .build();
    }
}