package com.payguard.fraud.service;

import com.payguard.fraud.dto.FraudScoreResult;
import com.payguard.fraud.dto.TransactionFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class RuleBasedScorer {

    @Value("${fraud.thresholds.approve-max}")
    private double approveMax;

    @Value("${fraud.thresholds.review-max}")
    private double reviewMax;

    @Value("${fraud.model.version}")
    private String modelVersion;

    /**
     * Score a transaction using deterministic rules.
     * Returns a score between 0.0 (safe) and 1.0 (fraud).
     */
    public FraudScoreResult score(String transactionId, TransactionFeatures features) {
        long startTime = System.currentTimeMillis();

        double score = 0.0;
        List<String> factors = new ArrayList<>();
        Map<String, Object> featureMap = buildFeatureMap(features);

        // Rule 1: Amount deviation from average (weight: 0.25)
        if (features.getUserAvgAmountCents() > 0) {
            double deviation = (double) features.getAmountCents() / features.getUserAvgAmountCents();
            if (deviation > 5.0) {
                score += 0.25;
                factors.add("extreme_amount_deviation");
            } else if (deviation > 3.0) {
                score += 0.15;
                factors.add("high_amount_deviation");
            } else if (deviation < 1.5) {
                factors.add("normal_amount");
            }
        }

        // Rule 2: High velocity — many transactions in short time (weight: 0.2)
        if (features.getTxVelocity1h() > 10) {
            score += 0.20;
            factors.add("extreme_velocity_1h");
        } else if (features.getTxVelocity1h() > 5) {
            score += 0.10;
            factors.add("high_velocity_1h");
        }

        // Rule 3: 24-hour velocity (weight: 0.1)
        if (features.getTxVelocity24h() > 50) {
            score += 0.10;
            factors.add("extreme_velocity_24h");
        }

        // Rule 4: Country mismatch (weight: 0.2)
        if (features.isCountryMismatch()) {
            score += 0.20;
            factors.add("country_mismatch");
        } else {
            factors.add("matching_country");
        }

        // Rule 5: New merchant — first time buyer (weight: 0.05)
        if (features.isNewMerchant()) {
            score += 0.05;
            factors.add("new_merchant");
        } else {
            factors.add("known_merchant");
        }

        // Rule 6: Failed attempts in last 24h (weight: 0.15)
        if (features.getFailedAttempts24h() > 5) {
            score += 0.15;
            factors.add("many_failed_attempts");
        } else if (features.getFailedAttempts24h() > 2) {
            score += 0.08;
            factors.add("some_failed_attempts");
        }

        // Rule 7: Unusual hour — transactions between 1AM-5AM local time (weight: 0.05)
        if (features.getHourOfDay() >= 1 && features.getHourOfDay() <= 5) {
            score += 0.05;
            factors.add("unusual_hour");
        }

        // Cap at 1.0
        score = Math.min(score, 1.0);

        // Determine decision
        String decision;
        if (score < approveMax) {
            decision = "APPROVE";
        } else if (score < reviewMax) {
            decision = "REVIEW";
        } else {
            decision = "BLOCK";
        }

        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        if (factors.isEmpty()) {
            factors.add("low_risk_profile");
        }

        return FraudScoreResult.builder()
                .transactionId(transactionId)
                .fraudScore(score)
                .decision(decision)
                .contributingFactors(factors)
                .modelVersion(modelVersion)
                .latencyMs(latencyMs)
                .fallbackUsed(false)
                .features(featureMap)
                .build();
    }

    private Map<String, Object> buildFeatureMap(TransactionFeatures f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("amount_cents", f.getAmountCents());
        map.put("hour_of_day", f.getHourOfDay());
        map.put("day_of_week", f.getDayOfWeek());
        map.put("merchant_category", f.getMerchantCategory());
        map.put("user_avg_amount_cents", f.getUserAvgAmountCents());
        map.put("amount_deviation", f.getAmountDeviation());
        map.put("tx_velocity_1h", f.getTxVelocity1h());
        map.put("tx_velocity_24h", f.getTxVelocity24h());
        map.put("is_new_merchant", f.isNewMerchant());
        map.put("country_mismatch", f.isCountryMismatch());
        map.put("failed_attempts_24h", f.getFailedAttempts24h());
        return map;
    }
}