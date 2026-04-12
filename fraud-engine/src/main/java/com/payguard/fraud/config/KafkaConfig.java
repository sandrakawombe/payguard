package com.payguard.fraud.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String FRAUD_SCORED_TOPIC = "fraud.scored";
    public static final String FRAUD_ALERT_HIGH_TOPIC = "fraud.alert.high";

    @Bean
    public NewTopic fraudScoredTopic() {
        return TopicBuilder.name(FRAUD_SCORED_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertHighTopic() {
        return TopicBuilder.name(FRAUD_ALERT_HIGH_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}