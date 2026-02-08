package com.company.bank_system.kafka;


import org.springframework.stereotype.Service;
import shared.dto.AiMessageRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnswerStore {

    private final Map<String, String> answers = new ConcurrentHashMap<>();

    public void save(AiMessageRequest aiMessageRequest) {
        answers.put(aiMessageRequest.requestId(), aiMessageRequest.text());
    }

    public String getById(String requestId) {
        return answers.get(requestId);
    }

}
