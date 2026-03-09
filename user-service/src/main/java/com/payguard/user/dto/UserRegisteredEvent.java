package com.payguard.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String source;
    private UserRegisteredData data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserRegisteredData {
        private UUID userId;
        private String email;
        private String merchantName;
        private String merchantCategory;
        private String country;
    }

    public static UserRegisteredEvent fromUser(UUID userId, String email,
                                                String merchantName,
                                                String merchantCategory,
                                                String country) {
        return UserRegisteredEvent.builder()
                .eventId("evt_" + UUID.randomUUID().toString().substring(0, 8))
                .eventType("user.registered")
                .timestamp(LocalDateTime.now())
                .source("user-service")
                .data(UserRegisteredData.builder()
                        .userId(userId)
                        .email(email)
                        .merchantName(merchantName)
                        .merchantCategory(merchantCategory)
                        .country(country)
                        .build())
                .build();
    }
}