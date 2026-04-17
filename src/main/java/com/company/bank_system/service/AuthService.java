package com.company.bank_system.service;

import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.EmailConfirmation;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.exception.Exceptions.*;
import com.company.bank_system.repo.EmailConfirmedRepository;
import com.company.bank_system.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final CurrentUserService currentUserService;
    private final TokenRevocationService tokenRevocationService;
    private final EmailAsyncService emailAsyncService;
    private final EmailConfirmedRepository emailConfirmedRepository;
    private static final int ATTEMPTS_FOR_EMAIL_CONFIRM = 5;
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTService jwtService, CurrentUserService currentUserService, TokenRevocationService tokenRevocationService, EmailAsyncService emailAsyncService, EmailConfirmedRepository emailConfirmedRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        ensureEmailNotExists(request.email());
        ensurePhoneNotExists(request.phone());

        User user = User.register(
                request.email(),
                request.phone(),
                request.firstName(),
                passwordEncoder.encode(request.password())
        );

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("REGISTER_FAILED_DUPLICATE email={} phone={}",
                    maskEmail(request.email()),
                    maskPhone(request.phone())
            );

            throw new UserAlreadyExistsException("User already exists");
        }

        log.info("REGISTER_SUCCESS userId={} email={}",
                user.getId(),
                maskEmail(user.getEmail())
        );

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
    }

    private void ensureEmailNotExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("REGISTER_FAILED_EMAIL_EXISTS email={}", maskEmail(email));
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }
    }

    private void ensurePhoneNotExists(String phone) {
        if (userRepository.findByPhone(phone).isPresent()) {
            log.warn("REGISTER_FAILED_PHONE_EXISTS phone={}", maskPhone(phone));
            throw new UserAlreadyExistsException("User with phone " + phone + " already exists");
        }
    }

    public String login(LoginRequest request) {

        log.info("LOGIN_START email={}", maskEmail(request.email()));

        User user = findUserByEmailOrThrow(request.email());

        assertUserActive(user);
        assertPasswordMatches(request.password(), user);

        log.info("LOGIN_SUCCESS userId={} email={}",
                user.getId(),
                maskEmail(user.getEmail())
        );

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("LOGIN_FAILED_USER_NOT_FOUND email={}", maskEmail(email));
                    return new UserNotFoundException("Incorrect email or password");
                });
    }

    private void assertUserActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("LOGIN_FAILED_USER_BLOCKED userId={} status={}",
                    user.getId(),
                    user.getStatus()
            );
            throw new AccessDeniedException("User account is blocked");
        }
    }

    private void assertPasswordMatches(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("LOGIN_FAILED_BAD_PASSWORD userId={} email={}",
                    user.getId(),
                    maskEmail(user.getEmail())
            );
            throw new UserNotFoundException("Incorrect email or password");
        }
    }

    @Transactional
    public void sendEmailKey(){
        UserCache currentUser = currentUserService.getCurrentUser();
        log.info("SEND_EMAIL_KEY_START userId={} email={}", currentUser.id(), currentUser.email());

        EmailConfirmation emailConfirmation = getOrCreateConfirmation(currentUser.id());
        validateCooldown(emailConfirmation);

        String mailKey = generateMailKey();
        log.info("Generated mail key for userId={}", currentUser.id());

        User userEntity = getUserEntity(currentUser.id());

        updateConfirmation(
                emailConfirmation,
                userEntity,
                mailKey
        );

        try {
            emailConfirmedRepository.save(emailConfirmation);
        } catch (DataIntegrityViolationException e) {
            log.warn("EMAIL_CONFIRMATION_SAVE_FAILED_DUPLICATE userId={} email={}",
                    currentUser.id(),
                    currentUser.email()
            );

            throw new EmailConfirmationException("Concurrent update detected");
        }

        log.info("Saved email confirmation state to DB for userId={}", currentUser.id());

        emailAsyncService.sendRegisterKeyEmail(currentUser.email(), mailKey);
        log.info("Delegated email sending to async service for email={}", maskEmail(currentUser.email()));
    }

    private EmailConfirmation getOrCreateConfirmation(Long userId) {
        return emailConfirmedRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        EmailConfirmation confirmation = new EmailConfirmation();
                        confirmation.setUser(getUserEntity(userId));
                        return emailConfirmedRepository.save(confirmation);
                    } catch (DataIntegrityViolationException e){
                        return emailConfirmedRepository.findByUserId(userId)
                                .orElseThrow();
                    }
                });
    }

    private void validateCooldown(EmailConfirmation confirmation) {
        LocalDateTime now = LocalDateTime.now();

        if (confirmation.getId() == null) return;

        if (confirmation.isUsed()) return;

        if (confirmation.getCreated_at() == null) return;

        LocalDateTime nextAllowedTime = confirmation.getCreated_at().plusMinutes(1);

        if (nextAllowedTime.isAfter(now)) {
            log.warn("SEND_EMAIL_KEY_COOLDOWN");
            throw new InvalidOperationException("Подождите минуту перед повторным запросом кода.");
        }
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void updateConfirmation(EmailConfirmation confirmation,
                                    User user,
                                    String mailKey) {

        LocalDateTime now = LocalDateTime.now();

        confirmation.setUser(user);
        confirmation.setMailKeyHash(passwordEncoder.encode(mailKey));
        confirmation.setCreated_at(now);
        confirmation.setExpires_at(now.plusMinutes(15));
        confirmation.setUsed(false);
        confirmation.setAttempts(0);
    }

    @Transactional
    @CacheEvict(value = "currentUser", key = "#root.target.getCurrentUserCacheKey()")
    public boolean isEmailKeyValid(String key){
        UserCache currentUser = currentUserService.getCurrentUser();
        EmailConfirmation confirmation = getConfirmationOrThrow(currentUser.id());

        if (!isConfirmationValid(confirmation)) {
            return false;
        }

        if (!isKeyCorrect(key, confirmation)) {
            incrementAttempts(confirmation);
            return false;
        }

        confirmEmail(confirmation, currentUser.id());

        return true;
    }

    private EmailConfirmation getConfirmationOrThrow(Long userId) {
        return emailConfirmedRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("EMAIL_CONFIRMATION_NOT_FOUND user_id={}", userId);
                    return new InvalidOperationException("Confirmation not found");
                });
    }

    private boolean isConfirmationValid(EmailConfirmation confirmation) {

        if (confirmation.isUsed()) {
            log.warn("EMAIL_CONFIRMATION_FAILED_ALREADY_USED userId={}",
                    confirmation.getUser().getId());
            return false;
        }

        if (confirmation.getExpires_at().isBefore(LocalDateTime.now())) {
            log.warn("EMAIL_CONFIRMATION_FAILED_EXPIRED userId={} expiresAt={}",
                    confirmation.getUser().getId(),
                    confirmation.getExpires_at());
            return false;
        }

        if (confirmation.getAttempts() >= ATTEMPTS_FOR_EMAIL_CONFIRM) {
            log.warn("EMAIL_CONFIRMATION_FAILED_TOO_MANY_ATTEMPTS userId={} attempts={}",
                    confirmation.getUser().getId(),
                    confirmation.getAttempts());
            return false;
        }

        return true;
    }

    private boolean isKeyCorrect(String key, EmailConfirmation confirmation) {
        String keyToMatch = key != null ? key.trim() : "";

        return passwordEncoder.matches(keyToMatch, confirmation.getMailKeyHash());
    }

    private void incrementAttempts(EmailConfirmation confirmation) {
        confirmation.setAttempts(confirmation.getAttempts() + 1);
        emailConfirmedRepository.save(confirmation);

        log.warn("EMAIL_CONFIRMATION_FAILED_BAD_CODE userId={} attempts={}",
                confirmation.getUser().getId(),
                confirmation.getAttempts());
    }

    private void confirmEmail(EmailConfirmation confirmation, Long userId) {

        confirmation.setUsed(true);
        emailConfirmedRepository.save(confirmation);

        User user = getUserEntity(userId);
        user.setConfirmed(true);
        userRepository.save(user);

        log.info("EMAIL_CONFIRMATION_SUCCESS userId={}", userId);
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

    @SuppressWarnings("unused")
    public String getCurrentUserCacheKey() {
        return currentUserService.getCurrentEmail();
    }

    public String generateMailKey(){
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
