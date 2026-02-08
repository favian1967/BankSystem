package com.company.bank_system.kafka;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/helper")
public class AiHelperController {

    private final AiHelperProducer aiHelperProducer;
    private final AnswerStore answerStore;

    public AiHelperController(AiHelperProducer aiHelperProducer, AnswerStore answerStore) {
        this.aiHelperProducer = aiHelperProducer;
        this.answerStore = answerStore;
    }

    @GetMapping("/ask")
    public String sendMessage(
            @RequestParam String message
    ) {
        log.info("REST received: {}", message);
        return aiHelperProducer.sendMessage(message);
    }
    @GetMapping("/answer/{id}")
    public String getAnswer(@PathVariable String id) {
        return answerStore.getById(id);
    }
}
