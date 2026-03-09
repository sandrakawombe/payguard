package com.payguard.user.service;

import com.payguard.user.config.KafkaConfig;
import com.payguard.user.dto.UserRegisteredEvent;
import com.payguard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.fromUser(
                user.getId(),
                user.getEmail(),
                user.getMerchantName(),
                user.getMerchantCategory(),
                user.getCountry()
        );

        kafkaTemplate.send(
                KafkaConfig.USER_REGISTERED_TOPIC,
                user.getId().toString(),
                event
        );

        log.info("Published user.registered event for user: {}", user.getId());
    }
}