package com.company.bank_system.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import shared.dto.AiMessageRequest;

import java.util.UUID;

@Slf4j
@Service
public class AiHelperProducer {
    private final KafkaTemplate<String, AiMessageRequest> kafkaTemplate;
    @Value("${app.kafka.topic.messages}")
    private String requestTopic;

    public AiHelperProducer(KafkaTemplate<String, AiMessageRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }



    public String sendMessage(String message) {
        String uuid = UUID.randomUUID().toString();
        AiMessageRequest msg = new AiMessageRequest(uuid, message);



        kafkaTemplate.send(requestTopic, msg)




                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed", ex);
                    } else {
                        log.info("Kafka send success: {}", message);
                        log.info("Sending to topic: [{}]", requestTopic);
                    }
                });

        return uuid;
    }


}
