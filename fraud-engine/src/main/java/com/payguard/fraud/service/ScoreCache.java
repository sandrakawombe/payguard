package com.payguard.fraud.service;

import com.payguard.fraud.dto.FraudScoreResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreCache {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fraud.cache.ttl-seconds}")
    private int ttlSeconds;

    public Optional<FraudScoreResult> get(String merchantId, long amountCents, String email) {
        String key = buildKey(merchantId, amountCents, email);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof FraudScoreResult result) {
                log.debug("Cache HIT for key: {}", key);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void put(String merchantId, long amountCents, String email, FraudScoreResult result) {
        String key = buildKey(merchantId, amountCents, email);
        try {
            redisTemplate.opsForValue().set(key, result, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached score for key: {}", key);
        } catch (Exception e) {
            log.warn("Redis cache write failed: {}", e.getMessage());
        }
    }

    private String buildKey(String merchantId, long amountCents, String email) {
        return "fraud:score:" + merchantId + ":" + amountCents + ":" + email;
    }
}