package com.payguard.user.dto;

import com.payguard.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String merchantName;
    private String merchantCategory;
    private String country;
    private String role;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .merchantName(user.getMerchantName())
                .merchantCategory(user.getMerchantCategory())
                .country(user.getCountry())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}