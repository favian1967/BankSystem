package com.company.bank_system.service;


import com.company.bank_system.dto.ChangePasswordRequest;
import com.company.bank_system.dto.ChangePasswordResponse;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.User;
import com.company.bank_system.exception.Exceptions.InvalidOperationException;
import com.company.bank_system.exception.Exceptions.UserNotFoundException;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @CacheEvict(value = "currentUser", key = "#root.target.getCurrentUserCacheKey()")
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {

        UserCache currentUser = currentUserService.getCurrentUser();

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(UserNotFoundException::new);

        String oldPassword = request.oldPassword();
        String newPassword = request.newPassword();
        String repeatPassword = request.repeatNewPassword();

        if (!newPassword.equals(repeatPassword)) {
            throw new InvalidOperationException("Passwords do not match");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidOperationException("Old password is incorrect");
        }

        if (oldPassword.equals(newPassword)) {
            throw new InvalidOperationException("Use a different password");
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
