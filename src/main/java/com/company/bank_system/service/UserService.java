package com.company.bank_system.service;


import com.company.bank_system.dto.ChangePasswordRequest;
import com.company.bank_system.dto.ChangePasswordResponse;
import com.company.bank_system.entity.User;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
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

    public ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        User user = currentUserService.getCurrentUser();

        String oldPassword = changePasswordRequest.oldPassword();
        String newPassword = changePasswordRequest.newPassword();
        String repeatPassword = changePasswordRequest.repeatNewPassword();

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return new ChangePasswordResponse(
                    user.getEmail(), "Old password is incorrect"
            );
        }

        if (oldPassword.equals(newPassword)) {
            return new ChangePasswordResponse(
                    user.getEmail(),"please, use password, which not used before"
            );
        }
        if (!newPassword.equals(repeatPassword)) {
            return new ChangePasswordResponse(
                    user.getEmail(),"New passwords do not match"
            );

        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return new ChangePasswordResponse(
                user.getEmail(),"Password has been changed"
        );
    }
}
