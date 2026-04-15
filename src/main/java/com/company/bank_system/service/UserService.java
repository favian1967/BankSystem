package com.company.bank_system.service;


import com.company.bank_system.dto.ChangePasswordRequest;
import com.company.bank_system.dto.ChangePasswordResponse;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.User;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @CacheEvict(value = "currentUser", key = "#root.target.getCurrentUserCacheKey()")
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {

        UserCache currentUser = currentUserService.getCurrentUser();

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldPassword = request.oldPassword();
        String newPassword = request.newPassword();
        String repeatPassword = request.repeatNewPassword();

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return new ChangePasswordResponse(user.getEmail(), "Old password is incorrect");
        }

        if (oldPassword.equals(newPassword)) {
            return new ChangePasswordResponse(user.getEmail(), "Use a different password");
        }

        if (!newPassword.equals(repeatPassword)) {
            return new ChangePasswordResponse(user.getEmail(), "Passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new ChangePasswordResponse(user.getEmail(), "Password has been changed");
    }

    @SuppressWarnings("unused")
    public String getCurrentUserCacheKey() {
        return currentUserService.getCurrentEmail();
    }
}
