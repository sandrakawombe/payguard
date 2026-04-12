package com.payguard.reconciliation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String RECONCILIATION_COMPLETED_TOPIC = "reconciliation.completed";

    @Bean
    public NewTopic reconciliationCompletedTopic() {
        return TopicBuilder.name(RECONCILIATION_COMPLETED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}