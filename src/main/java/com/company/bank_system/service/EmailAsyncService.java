package com.company.bank_system.service;

import com.company.bank_system.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailAsyncService {
    private final MailSenderService mailSenderService;

    public EmailAsyncService(MailSenderService mailSenderService) {
        this.mailSenderService = mailSenderService;
    }


    @Async
    public void sendRegisterKeyEmail(String email, String mailKey) {
        log.info("EMAIL THREAD = " + Thread.currentThread().getName());
        mailSenderService.send(email, "Your register key", mailKey);
    }

}
