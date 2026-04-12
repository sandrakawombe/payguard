package com.payguard.notification.service;

import com.payguard.notification.entity.Notification;
import com.payguard.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationProcessor notificationProcessor;

    @Test
    void shouldSendNotificationSuccessfully() {
        ReflectionTestUtils.setField(notificationProcessor, "maxAttempts", 3);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type("PAYMENT_RECEIPT").channel("EMAIL").status("PENDING")
                .recipient("test@test.com").subject("Test")
                .payload(Map.of("key", "value")).attempts(0).build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        notificationProcessor.processNotification(
                UUID.randomUUID(), "test@test.com",
                "PAYMENT_RECEIPT", "Test Subject", "Test body",
                Map.of("key", "value"));

        verify(emailService, times(1)).sendEmail("test@test.com", "Test Subject", "Test body");
    }

    @Test
    void shouldRetryAndEventuallySucceed() {
        ReflectionTestUtils.setField(notificationProcessor, "maxAttempts", 3);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type("WELCOME").channel("EMAIL").status("PENDING")
                .recipient("test@test.com").subject("Welcome")
                .payload(Map.of("key", "value")).attempts(0).build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        doThrow(new RuntimeException("SMTP down"))
                .doNothing()
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        notificationProcessor.processNotification(
                UUID.randomUUID(), "test@test.com",
                "WELCOME", "Welcome", "Welcome body",
                Map.of("key", "value"));

        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
    }
}