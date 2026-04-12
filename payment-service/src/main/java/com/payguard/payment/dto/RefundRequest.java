package com.payguard.payment.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Min(value = 1, message = "Refund amount must be at least 1 cent")
    private long amount;

    private String reason;
}