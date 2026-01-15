package com.company.bank_system.controller;

import com.company.bank_system.dto.ConfirmRequest;
import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.service.AuthService;
import com.company.bank_system.service.CurrentUserService;
import com.company.bank_system.service.MailSenderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MailSenderService mailSenderService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, MailSenderService mailSenderService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.mailSenderService = mailSenderService;
        this.currentUserService = currentUserService;
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

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
            return Map.of("message", "Logged out successfully");
        }
        return Map.of("message", "Authentication Failed");
    }

    @PostMapping("/confirm")
    public boolean confirm(@RequestBody ConfirmRequest request){
        return authService.isEmailKeyValid(request.key());
    }
}