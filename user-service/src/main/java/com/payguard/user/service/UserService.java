package com.payguard.user.service;

import com.payguard.user.dto.*;
import com.payguard.user.entity.User;
import com.payguard.user.exception.*;
import com.payguard.user.repository.UserRepository;
import com.payguard.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .merchantName(request.getMerchantName().trim())
                .merchantCategory(request.getMerchantCategory().trim().toLowerCase())
                .country(request.getCountry().trim().toUpperCase())
                .role(User.Role.MERCHANT)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New merchant registered: {} ({})", savedUser.getEmail(), savedUser.getId());

        // Publish event to Kafka — Notification Service will send welcome email
        eventPublisher.publishUserRegistered(savedUser);

        String token = jwtTokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("Merchant logged in: {} ({})", user.getEmail(), user.getId());

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setMerchantName(request.getMerchantName().trim());
        user.setMerchantCategory(request.getMerchantCategory().trim().toLowerCase());
        user.setCountry(request.getCountry().trim().toUpperCase());

        User updatedUser = userRepository.save(user);
        log.info("Merchant profile updated: {} ({})", updatedUser.getEmail(), updatedUser.getId());

        return UserResponse.fromEntity(updatedUser);
    }

    /**
     * Internal method — used by other services via REST to get merchant details
     * for fraud scoring feature assembly.
     */
    @Transactional(readOnly = true)
    public UserResponse getMerchantById(UUID merchantId) {
        User user = userRepository.findById(merchantId)
                .orElseThrow(() -> new UserNotFoundException(merchantId));
        return UserResponse.fromEntity(user);
    }
}