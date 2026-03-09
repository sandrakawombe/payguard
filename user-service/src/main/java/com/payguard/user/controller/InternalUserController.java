package com.payguard.user.controller;

import com.payguard.user.dto.UserResponse;
import com.payguard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * Internal endpoint for service-to-service calls.
     * Used by Fraud Engine to fetch merchant profile for feature assembly.
     * NOT exposed through API Gateway.
     */
    @GetMapping("/{merchantId}")
    public ResponseEntity<UserResponse> getMerchantProfile(@PathVariable UUID merchantId) {
        UserResponse response = userService.getMerchantById(merchantId);
        return ResponseEntity.ok(response);
    }
}