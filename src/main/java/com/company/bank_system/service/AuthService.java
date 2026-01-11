package com.company.bank_system.service;

import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.exception.Exceptions.UserAlreadyExistsException;
import com.company.bank_system.exception.Exceptions.UserNotFoundException;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final MailSenderService  mailSenderService;
    private final CurrentUserService currentUserService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTService jwtService, MailSenderService mailSenderService, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSenderService = mailSenderService;
        this.currentUserService = currentUserService;
    }

    public String register(RegisterRequest request) {

        log.info("REGISTER_START email={} phone={}",
                maskEmail(request.email()),
                maskPhone(request.phone())
        );

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("REGISTER_FAILED_EMAIL_EXISTS email={}",
                    maskEmail(request.email())
            );
            throw new UserAlreadyExistsException(
                    "User with email " + request.email() + " already exists"
            );
        }

        if (userRepository.findByPhone(request.phone()).isPresent()) {
            log.warn("REGISTER_FAILED_PHONE_EXISTS phone={}",
                    maskPhone(request.phone())
            );
            throw new UserAlreadyExistsException(
                    "User with phone " + request.phone() + " already exists"
            );
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(UserRole.USER);
        user.setConfirmed(false);



        userRepository.save(user);

        log.info("REGISTER_SUCCESS userId={} email={}",
                user.getId(),
                maskEmail(user.getEmail())
        );

        return jwtService.generateToken(user.getEmail());
    }

    public String login(LoginRequest request) {

        log.info("LOGIN_START email={}", maskEmail(request.email()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("LOGIN_FAILED_USER_NOT_FOUND email={}",
                            maskEmail(request.email())
                    );
                    return new UserNotFoundException(
                            "User with email " + request.email() + " not found"
                    );
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("LOGIN_FAILED_BAD_PASSWORD userId={} email={}",
                    user.getId(),
                    maskEmail(user.getEmail())
            );
            throw new UserNotFoundException("Incorrect email or password");
        }

        log.info("LOGIN_SUCCESS userId={} email={}",
                user.getId(),
                maskEmail(user.getEmail())
        );

        return jwtService.generateToken(user.getEmail());
    }

    public void sendEmailKey(){
        User currentUser = currentUserService.getCurrentUser();

        String mailKey = generateMailKey();
        currentUser.setMailKey(mailKey);
        mailSenderService.send(
                currentUser.getEmail(),
                "Your register key",
                mailKey
        );
        userRepository.save(currentUser);
    }

    public boolean isEmailKeyValid(String key){
        User currentUser = currentUserService.getCurrentUser();
        String currentKey = currentUser.getMailKey();
        if (currentKey == null || !currentKey.equals(key)) {
            throw new UserNotFoundException("Incorrect key");
        }
        currentUser.setConfirmed(true);
        currentUser.setMailKey(null);
        userRepository.save(currentUser);
        return true;
    }



    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 2) return "***@***";
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 3);
    }

    private String generateMailKey(){
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
