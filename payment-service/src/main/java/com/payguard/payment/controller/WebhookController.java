package com.payguard.payment.controller;

import com.payguard.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Stripe webhook endpoint.
     * In production, verify the Stripe signature header.
     * For development, we accept the payload directly.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("type");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> object = (Map<String, Object>) data.get("object");

            String paymentIntentId = (String) object.get("id");
            String status = (String) object.get("status");

            log.info("Received Stripe webhook: type={}, paymentIntent={}", eventType, paymentIntentId);

            paymentService.handleStripeWebhook(eventType, paymentIntentId, status);

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.ok("Webhook received");
        }
    }
}