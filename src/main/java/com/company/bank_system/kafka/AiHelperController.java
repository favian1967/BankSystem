package com.company.bank_system.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/helper")
@Validated
public class AiHelperController {

    private final AiHelperProducer aiHelperProducer;
    private final AnswerStore answerStore;

    public AiHelperController(AiHelperProducer aiHelperProducer, AnswerStore answerStore) {
        this.aiHelperProducer = aiHelperProducer;
        this.answerStore = answerStore;
    }

    @GetMapping("/ask")
    public ResponseEntity<String> sendMessage(
            @RequestParam String message
    ) {
        log.info("REST received: {}", message);
        return ResponseEntity.ok(aiHelperProducer.sendMessage(message));
    }

    @GetMapping("/answer/{id}")
    public ResponseEntity<String> getAnswer(@PathVariable String id) {
        return ResponseEntity.ok(answerStore.getById(id));
    }
}
