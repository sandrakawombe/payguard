package com.payguard.notification.controller;

import com.payguard.notification.entity.Notification;
import com.payguard.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<Notification>> getFailed() {
        return ResponseEntity.ok(notificationRepository.findByStatus("FAILED"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long total = notificationRepository.count();
        long failed = notificationRepository.findByStatus("FAILED").size();
        long sent = notificationRepository.findByStatus("SENT").size();
        return ResponseEntity.ok(Map.of("total", total, "sent", sent, "failed", failed));
    }
}