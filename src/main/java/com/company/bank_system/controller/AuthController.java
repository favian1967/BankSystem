package com.company.bank_system.controller;

import com.company.bank_system.dto.ConfirmRequest;
import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.service.AuthService;
import com.company.bank_system.service.MailSenderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MailSenderService mailSenderService;

    public AuthController(AuthService authService, MailSenderService mailSenderService) {
        this.authService = authService;
        this.mailSenderService = mailSenderService;
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @PostMapping("/send")
    public void send(

    ){
        authService.sendEmailKey();
    }


    @PostMapping("/confirm")
    public boolean confirm(@RequestBody ConfirmRequest request){
        return authService.isEmailKeyValid(request.key());
    }
}