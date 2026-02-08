package com.company.bank_system.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.messages}")
    private String requestTopic;
    @Value("${app.kafka.topic.answers}")
    private String responseTopic;


    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(requestTopic)
                .partitions(3)
                .replicas(1) // 1 broker - 1 repl
                .build();
    }

    @Bean
    public NewTopic newTopic() {
        return TopicBuilder.name(responseTopic)
                .partitions(3)
                .replicas(1) // 1 broker - 1 repl
                .build();
    }

}