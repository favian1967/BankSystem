package com.company.bank_system.service;

import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.entity.EmailConfirmation;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.exception.Exceptions.*;
import com.company.bank_system.repo.EmailConfirmedRepository;
import com.company.bank_system.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final MailSenderService  mailSenderService;
    private final CurrentUserService currentUserService;
    private final TokenRevocationService tokenRevocationService;
    private final EmailAsyncService emailAsyncService;
    private final EmailConfirmedRepository emailConfirmedRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTService jwtService, MailSenderService mailSenderService, CurrentUserService currentUserService, TokenRevocationService tokenRevocationService, EmailAsyncService emailAsyncService, EmailConfirmedRepository emailConfirmedRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSenderService = mailSenderService;
        this.currentUserService = currentUserService;
        this.tokenRevocationService = tokenRevocationService;
        this.emailAsyncService = emailAsyncService;
        this.emailConfirmedRepository = emailConfirmedRepository;
    }

    public void logout(String token) {
        tokenRevocationService.revoke(token);
    }
    public boolean isTokenRevoked(String token) {
        return tokenRevocationService.isRevoked(token);
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

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
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

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("LOGIN_FAILED_USER_BLOCKED userId={} status={}",
                    user.getId(),
                    user.getStatus()
            );
            throw new AccessDeniedException("User account is blocked");
        }


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

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void sendEmailKey(){
        User currentUser = currentUserService.getCurrentUser();
        log.info("SEND_EMAIL_KEY_START userId={} email={}", currentUser.getId(), currentUser.getEmail());

        EmailConfirmation emailConfirmation = emailConfirmedRepository.findByUserId(currentUser.getId())
                .orElseGet(EmailConfirmation::new);

        LocalDateTime now = LocalDateTime.now();
        if (emailConfirmation.getId() != null && !emailConfirmation.isUsed()
                && emailConfirmation.getCreated_at() != null) {
            LocalDateTime nextAllowedTime = emailConfirmation.getCreated_at().plusMinutes(1);
            // Verify nextAllowedTime is after now, but also ensure it's not unreasonably far in the future
            // which could indicate a timezone mismatch bug locking the user out.
            if (nextAllowedTime.isAfter(now) && emailConfirmation.getCreated_at().isBefore(now.plusMinutes(5))) {
                log.warn("SEND_EMAIL_KEY_COOLDOWN userId={}", currentUser.getId());
                throw new InvalidOperationException("Подождите минуту перед повторным запросом кода.");
            }
        }

        String mailKey = generateMailKey();
        log.info("Generated mail key for userId={}", currentUser.getId());

        emailConfirmation.setUser(currentUser);
        emailConfirmation.setMailKeyHash(passwordEncoder.encode(mailKey));
        emailConfirmation.setCreated_at(now);
        emailConfirmation.setExpires_at(now.plusMinutes(15));
        emailConfirmation.setUsed(false);
        emailConfirmation.setAttempts(0);

        emailConfirmedRepository.save(emailConfirmation);
        log.info("Saved email confirmation state to DB for userId={}", currentUser.getId());

        emailAsyncService.sendRegisterKeyEmail(currentUser.getEmail(), mailKey);
        log.info("Delegated email sending to async service for email={}", maskEmail(currentUser.getEmail()));
    }

    @Transactional
    public boolean isEmailKeyValid(String key){
        User currentUser = currentUserService.getCurrentUser();
        EmailConfirmation emailConfirmation = emailConfirmedRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.warn("EMAIL_CONFIRMATION_NOT_FOUND user_id={}", currentUser.getId());
                    return new InvalidOperationException(currentUser.getEmail());
                });

        String mailKeyHash = emailConfirmation.getMailKeyHash();

        if (emailConfirmation.isUsed()) {
            log.warn("EMAIL_CONFIRMATION_FAILED_ALREADY_USED userId={}",
                    currentUser.getId()
            );
            return false;
        }

        if (emailConfirmation.getExpires_at().isBefore(LocalDateTime.now())) {
            log.warn("EMAIL_CONFIRMATION_FAILED_EXPIRED userId={} expiresAt={}",
                    currentUser.getId(),
                    emailConfirmation.getExpires_at()
            );
            return false;
        }

        if (emailConfirmation.getAttempts() >= 5) {
            log.warn("EMAIL_CONFIRMATION_FAILED_TOO_MANY_ATTEMPTS userId={} attempts={}",
                    currentUser.getId(),
                    emailConfirmation.getAttempts()
            );
            return false;
        }

        String keyToMatch = key != null ? key.trim() : "";

        if (!passwordEncoder.matches(keyToMatch, mailKeyHash)) {
            emailConfirmation.setAttempts(emailConfirmation.getAttempts() + 1);
            emailConfirmedRepository.save(emailConfirmation);
            log.warn("EMAIL_CONFIRMATION_FAILED_BAD_CODE userId={} attempts={}",
                    currentUser.getId(),
                    emailConfirmation.getAttempts()
            );
            return false;
        }

        emailConfirmation.setUsed(true);
        emailConfirmedRepository.save(emailConfirmation);

        currentUser.setConfirmed(true);
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
