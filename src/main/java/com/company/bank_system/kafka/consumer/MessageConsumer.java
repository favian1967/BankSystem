package com.company.bank_system.kafka.consumer;

import com.company.bank_system.kafka.AnswerStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import shared.dto.AiMessageRequest;

@Service
public class MessageConsumer {

    private final AnswerStore answerStore;

    public MessageConsumer(AnswerStore answerStore) {
        this.answerStore = answerStore;
    }


    @KafkaListener(
            topics = "${app.kafka.topic.answers}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void receiveMessage(AiMessageRequest message) {
        answerStore.save(message);
    }
}
