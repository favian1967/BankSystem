package com.company.bank_system.service;


import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.entity.User;
import com.company.bank_system.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;


    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email уже занят");
        }

        // Проверка на существующий телефон (если есть метод)
        if (userRepository.findByPhone(request.phone()).isPresent()) {
            throw new RuntimeException("Телефон уже занят");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setPhone(request.phone());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return jwtService.generateToken(user.getEmail());

    }


    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Юзер не найден"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password");
        }


        return jwtService.generateToken(user.getEmail());
    }


}
