package com.payguard.user.service;

import com.payguard.user.dto.*;
import com.payguard.user.entity.User;
import com.payguard.user.exception.EmailAlreadyExistsException;
import com.payguard.user.exception.InvalidCredentialsException;
import com.payguard.user.repository.UserRepository;
import com.payguard.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldCreateUserAndReturnToken() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("walmart@test.com")
                .password("securePass123")
                .merchantName("Walmart Inc.")
                .merchantCategory("retail")
                .country("USA")
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("walmart@test.com")
                .passwordHash("hashed_password")
                .merchantName("Walmart Inc.")
                .merchantCategory("retail")
                .country("USA")
                .role(User.Role.MERCHANT)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                .thenReturn("jwt_token_123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        doNothing().when(eventPublisher).publishUserRegistered(any(User.class));

        // When
        AuthResponse response = userService.register(request);

        // Then
        assertThat(response.getToken()).isEqualTo("jwt_token_123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("walmart@test.com");
        assertThat(response.getUser().getRole()).isEqualTo("MERCHANT");

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserRegistered(any(User.class));
        verify(passwordEncoder).encode("securePass123");
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .password("securePass123")
                .merchantName("Existing Store")
                .merchantCategory("retail")
                .country("USA")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishUserRegistered(any());
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("walmart@test.com")
                .password("securePass123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("walmart@test.com")
                .passwordHash("hashed_password")
                .merchantName("Walmart Inc.")
                .merchantCategory("retail")
                .country("USA")
                .role(User.Role.MERCHANT)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("securePass123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                .thenReturn("jwt_token_456");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        // When
        AuthResponse response = userService.login(request);

        // Then
        assertThat(response.getToken()).isEqualTo("jwt_token_456");
        assertThat(response.getUser().getEmail()).isEqualTo("walmart@test.com");
    }

    @Test
    void login_shouldThrowForWrongPassword() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("walmart@test.com")
                .password("wrongPassword")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("walmart@test.com")
                .passwordHash("hashed_password")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldThrowForNonExistentEmail() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("nobody@test.com")
                .password("anyPassword")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}