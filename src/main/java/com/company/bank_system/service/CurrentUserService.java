package com.company.bank_system.service;

import com.company.bank_system.entity.User;
import com.company.bank_system.exception.Exceptions.UserNotFoundException;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public String getCurrentEmail() { //from jwt token
        log.debug("GET_CURRENT_EMAIL_START");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("GET_CURRENT_EMAIL_FAILED authentication=null");
            throw new IllegalStateException("No authenticated user in security context");
        }

        String email = authentication.getName();

        log.debug("GET_CURRENT_EMAIL_SUCCESS email={}", email);

        return email;
    }


    public User getCurrentUser() { //get authorized user entity
        log.debug("GET_CURRENT_USER_START");

        String email = getCurrentEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("CURRENT_USER_NOT_FOUND email={}", email);
                    return new UserNotFoundException("User with email " + email + " not found");
                });

        log.debug("GET_CURRENT_USER_SUCCESS userId={} email={} role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return user;
    }
}