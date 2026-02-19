package com.company.bank_system.service;

import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.UserAlreadyExistsException;
import com.company.bank_system.exception.Exceptions.UserNotFoundException;
import com.company.bank_system.repo.EmailConfirmedRepository;
import com.company.bank_system.repo.RevokedTokenRepository;
import com.company.bank_system.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JWTService jwtService;
    @Mock
    private MailSenderService mailSenderService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private TokenRevocationService tokenRevocationService;
    @Mock
    private EmailAsyncService emailAsyncService;
    @Mock
    private EmailConfirmedRepository emailConfirmedRepository;

    private User existingUser;

    @BeforeEach
    public void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("existing@test.com");
        existingUser.setPasswordHash("hashedPassword");
        existingUser.setFirstName("John");
        existingUser.setPhone("+79991234567");
        existingUser.setRole(UserRole.USER);
        existingUser.setStatus(UserStatus.ACTIVE);
        existingUser.setConfirmed(true);
    }

    // ==================== REGISTER ====================

    @Test
    public void register_shouldRegisterSuccessfully() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "new@test.com", "password123", "Ivan", "+79991234568"
        );

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79991234568")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(jwtService.generateToken("new@test.com")).thenReturn("jwt-token");

        // ACT
        String token = authService.register(request);

        // ASSERT
        assertEquals("jwt-token", token);
        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals("new@test.com") &&
                        user.getFirstName().equals("Ivan") &&
                        user.getPhone().equals("+79991234568") &&
                        user.getRole() == UserRole.USER &&
                        !user.isConfirmed()
        ));
        verify(jwtService, times(1)).generateToken("new@test.com");
    }

    @Test
    public void register_shouldFailWhenEmailExists() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "existing@test.com", "password123", "Ivan", "+79991234568"
        );

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));

        // ACT & ASSERT
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void register_shouldFailWhenPhoneExists() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "new@test.com", "password123", "Ivan", "+79991234567"
        );

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+79991234567")).thenReturn(Optional.of(existingUser));

        // ACT & ASSERT
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // ==================== LOGIN ====================

    @Test
    public void login_shouldLoginSuccessfully() {
        // ARRANGE
        LoginRequest request = new LoginRequest("existing@test.com", "password123");

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("existing@test.com")).thenReturn("jwt-token");

        // ACT
        String token = authService.login(request);

        // ASSERT
        assertEquals("jwt-token", token);
        verify(jwtService, times(1)).generateToken("existing@test.com");
    }

    @Test
    public void login_shouldFailWhenUserNotFound() {
        // ARRANGE
        LoginRequest request = new LoginRequest("nobody@test.com", "password123");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class, () -> authService.login(request));
    }

    @Test
    public void login_shouldFailWithWrongPassword() {
        // ARRANGE
        LoginRequest request = new LoginRequest("existing@test.com", "wrongPassword");

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class, () -> authService.login(request));
    }

    @Test
    public void login_shouldFailWhenUserIsBlocked() {
        // ARRANGE
        existingUser.setStatus(UserStatus.BLOCKED);
        LoginRequest request = new LoginRequest("existing@test.com", "password123");

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> authService.login(request));
    }

    // ==================== LOGOUT ====================

    @Test
    public void logout_shouldRevokeToken() {
        // ACT
        authService.logout("some-token");

        // ASSERT
        verify(tokenRevocationService, times(1)).revoke("some-token");
    }

    // ==================== TOKEN REVOKED CHECK ====================

    @Test
    public void isTokenRevoked_shouldReturnTrueWhenRevoked() {
        // ARRANGE
        when(revokedTokenRepository.existsByToken("revoked-token")).thenReturn(true);

        // ACT & ASSERT
        assertTrue(authService.isTokenRevoked("revoked-token"));
    }

    @Test
    public void isTokenRevoked_shouldReturnFalseWhenNotRevoked() {
        // ARRANGE
        when(revokedTokenRepository.existsByToken("valid-token")).thenReturn(false);

        // ACT & ASSERT
        assertFalse(authService.isTokenRevoked("valid-token"));
    }
}
