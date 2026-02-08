package com.company.bank_system.kafka;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/helper")
public class AiHelperController {

    private final AiHelperProducer aiHelperProducer;

    public AiHelperController(AiHelperProducer aiHelperProducer) {
        this.aiHelperProducer = aiHelperProducer;
    }

    @GetMapping("/send")
    public ResponseEntity<String> sendMessage(
            @RequestParam String message
    ) {
        log.info("REST received: {}", message);
        aiHelperProducer.sendMessage(message);
        return ResponseEntity.ok("Сообщение успешно отправлено в Kafka");
    }
}
