package com.company.bank_system.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiHelperProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    @Value("${app.kafka.topic.messages}")
    private String topicName;

    public AiHelperProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {
        kafkaTemplate.send(topicName, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed", ex);
                    } else {
                        log.info("Kafka send success: {}", message);
                        log.info("Sending to topic: [{}]", topicName);
                    }
                });
    }


}
