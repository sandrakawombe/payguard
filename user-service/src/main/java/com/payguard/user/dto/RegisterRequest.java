package com.payguard.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Merchant category is required")
    private String merchantCategory;

    @NotBlank(message = "Country is required")
    @Size(min = 3, max = 3, message = "Country must be a 3-letter ISO code")
    private String country;
}