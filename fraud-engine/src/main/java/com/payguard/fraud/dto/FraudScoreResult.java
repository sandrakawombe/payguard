package com.payguard.fraud.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudScoreResult {

    private String transactionId;
    private double fraudScore;
    private String decision;
    private List<String> contributingFactors;
    private String modelVersion;
    private int latencyMs;
    private boolean fallbackUsed;
    private Map<String, Object> features;
}