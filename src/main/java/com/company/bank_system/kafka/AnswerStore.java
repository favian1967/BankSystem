package com.company.bank_system.kafka;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Service;
import shared.dto.AiMessageRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
public class AnswerStore {

//    private final Map<String, String> answers = new ConcurrentHashMap<>();
    private final Cache<String, String> answers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public void save(AiMessageRequest aiMessageRequest) {
        answers.put(aiMessageRequest.requestId(), aiMessageRequest.text());


        log.info("ANSWER_SAVED requestId={} cacheSize={}",
                aiMessageRequest.requestId(),
                answers.estimatedSize());
    }

    public String getById(String requestId) {
        String result = answers.getIfPresent(requestId);;
        log.info("ANSWER_FETCH requestId={} found={} cacheSize={}",
                requestId,
                result != null,
                answers.estimatedSize());
        return result;
    }

}
