package com.payguard.fraud.service;

import com.payguard.fraud.dto.FraudScoreResult;
import com.payguard.fraud.dto.TransactionFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedScorerTest {

    private RuleBasedScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new RuleBasedScorer();
        ReflectionTestUtils.setField(scorer, "approveMax", 0.3);
        ReflectionTestUtils.setField(scorer, "reviewMax", 0.7);
        ReflectionTestUtils.setField(scorer, "modelVersion", "rules-v1.0.0-test");
    }

    @Test
    void shouldApproveNormalTransaction() {
        TransactionFeatures features = TransactionFeatures.builder()
                .amountCents(5000)
                .hourOfDay(14)
                .dayOfWeek(4)
                .merchantCategory("retail")
                .userAvgAmountCents(4500)
                .amountDeviation(1.11)
                .txVelocity1h(0)
                .txVelocity24h(2)
                .newMerchant(false)
                .countryMismatch(false)
                .failedAttempts24h(0)
                .build();

        FraudScoreResult result = scorer.score("txn_1", features);

        assertThat(result.getDecision()).isEqualTo("APPROVE");
        assertThat(result.getFraudScore()).isLessThan(0.3);
        assertThat(result.getContributingFactors()).contains("normal_amount");
    }

    @Test
    void shouldBlockHighRiskTransaction() {
        TransactionFeatures features = TransactionFeatures.builder()
                .amountCents(500000)
                .hourOfDay(3)
                .dayOfWeek(1)
                .merchantCategory("electronics")
                .userAvgAmountCents(5000)
                .amountDeviation(100.0)
                .txVelocity1h(12)
                .txVelocity24h(60)
                .newMerchant(true)
                .countryMismatch(true)
                .failedAttempts24h(8)
                .build();

        FraudScoreResult result = scorer.score("txn_2", features);

        assertThat(result.getDecision()).isEqualTo("BLOCK");
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.getContributingFactors()).contains("extreme_amount_deviation");
        assertThat(result.getContributingFactors()).contains("country_mismatch");
    }

    @Test
    void shouldReviewMediumRiskTransaction() {
        TransactionFeatures features = TransactionFeatures.builder()
                .amountCents(25000)
                .hourOfDay(14)
                .dayOfWeek(3)
                .merchantCategory("retail")
                .userAvgAmountCents(5000)
                .amountDeviation(5.0)
                .txVelocity1h(6)
                .txVelocity24h(10)
                .newMerchant(false)
                .countryMismatch(false)
                .failedAttempts24h(0)
                .build();

        FraudScoreResult result = scorer.score("txn_3", features);

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getFraudScore()).isBetween(0.3, 0.7);
    }
}