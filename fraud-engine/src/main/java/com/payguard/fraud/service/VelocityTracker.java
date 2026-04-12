package com.payguard.fraud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityTracker {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Increment and get the transaction count for a customer email in a time window.
     */
    public int incrementAndGet(String email, String window, Duration ttl) {
        String key = "fraud:velocity:" + window + ":" + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // First increment — set the TTL
            redisTemplate.expire(key, ttl);
        }
        return count != null ? count.intValue() : 0;
    }

    /**
     * Get current velocity count without incrementing.
     */
    public int getCount(String email, String window) {
        String key = "fraud:velocity:" + window + ":" + email;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return 0;
    }

    /**
     * Get failed attempt count.
     */
    public int getFailedAttempts(String email) {
        return getCount(email, "failed:24h");
    }

    /**
     * Increment failed attempt counter.
     */
    public void recordFailedAttempt(String email) {
        incrementAndGet(email, "failed:24h", Duration.ofHours(24));
    }
}