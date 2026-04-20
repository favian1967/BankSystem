package com.company.bank_system.service;

import com.company.bank_system.dto.ChangePasswordRequest;
import com.company.bank_system.dto.ChangePasswordResponse;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;
    private UserCache userCache;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setRole(UserRole.USER);
        user.setPasswordHash("hashedOldPassword");

        userCache = new UserCache(
                1L,
                "test@test.com",
                UserRole.USER.toString(),
                true,
                UserStatus.ACTIVE
        );
    }

    @Test
    public void changePassword_shouldChangeSuccessfully() {
        // ARRANGE
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword", "newPassword123", "newPassword123"
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");

        // ACT
        ChangePasswordResponse response = userService.changePassword(request);

        // ASSERT
        assertEquals("test@test.com", response.email());
        assertEquals("Password has been changed", response.message());
        verify(userRepository, times(1)).save(user);
        assertEquals("hashedNewPassword", user.getPasswordHash());
    }

    @Test
    public void changePassword_shouldFailWithWrongOldPassword() {
        // ARRANGE
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrongOldPassword", "newPassword123", "newPassword123"
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(passwordEncoder.matches("wrongOldPassword", "hashedOldPassword")).thenReturn(false);

        // ACT
        ChangePasswordResponse response = userService.changePassword(request);

        // ASSERT
        assertEquals("Old password is incorrect", response.message());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void changePassword_shouldFailWhenNewPasswordSameAsOld() {
        // ARRANGE
        ChangePasswordRequest request = new ChangePasswordRequest(
                "samePassword", "samePassword", "samePassword"
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(passwordEncoder.matches("samePassword", "hashedOldPassword")).thenReturn(true);

        // ACT
        ChangePasswordResponse response = userService.changePassword(request);

        // ASSERT
        assertEquals("please, use password, which not used before", response.message());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void changePassword_shouldFailWhenPasswordsDoNotMatch() {
        // ARRANGE
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword", "newPassword123", "differentPassword"
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);

        // ACT
        ChangePasswordResponse response = userService.changePassword(request);

        // ASSERT
        assertEquals("New passwords do not match", response.message());
        verify(userRepository, never()).save(any());
    }
}
