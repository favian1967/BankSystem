package com.company.bank_system.controller;

import com.company.bank_system.dto.LoginRequest;
import com.company.bank_system.dto.RegisterRequest;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.repo.RevokedTokenRepository;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.service.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ai_messages", "bank_ai_answers"})
class AuthControllerTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Autowired
    AuthControllerTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            RevokedTokenRepository revokedTokenRepository,
            JWTService jwtService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void setUp() {
        revokedTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== REGISTER ====================

    @Test
    void register_shouldRegisterAndReturnToken() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "newuser@test.com",
                "password123",
                "Ivan",
                "+79991234567"
        );

        // ACT & ASSERT
        String token = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(token).isNotBlank();
        assertThat(userRepository.findByEmail("newuser@test.com")).isPresent();

        User savedUser = userRepository.findByEmail("newuser@test.com").orElseThrow();
        assertThat(savedUser.getFirstName()).isEqualTo("Ivan");
        assertThat(savedUser.getPhone()).isEqualTo("+79991234567");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.isConfirmed()).isFalse();
    }

    @Test
    void register_shouldFailWhenEmailAlreadyExists() throws Exception {
        // ARRANGE
        createUser("existing@test.com", "+79991234567");

        RegisterRequest request = new RegisterRequest(
                "existing@test.com",
                "password123",
                "Ivan",
                "+79991234568"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void register_shouldFailWhenPhoneAlreadyExists() throws Exception {
        // ARRANGE
        createUser("first@test.com", "+79991234567");

        RegisterRequest request = new RegisterRequest(
                "second@test.com",
                "password123",
                "Ivan",
                "+79991234567"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void register_shouldFailWithInvalidEmail() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "not-an-email",
                "password123",
                "Ivan",
                "+79991234567"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void register_shouldFailWithShortPassword() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "test@test.com",
                "short",
                "Ivan",
                "+79991234567"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void register_shouldFailWithInvalidPhone() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "test@test.com",
                "password123",
                "Ivan",
                "12345"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(userRepository.count()).isZero();
    }

    // ==================== LOGIN ====================

    @Test
    void login_shouldReturnTokenWhenCredentialsAreValid() throws Exception {
        // ARRANGE
        createUser("login@test.com", "+79991234567");

        LoginRequest request = new LoginRequest(
                "login@test.com",
                "password123"
        );

        // ACT & ASSERT
        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(token).isNotBlank();
    }

    @Test
    void login_shouldFailWithWrongPassword() throws Exception {
        // ARRANGE
        createUser("login@test.com", "+79991234567");

        LoginRequest request = new LoginRequest(
                "login@test.com",
                "wrongpassword"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_shouldFailWhenUserNotFound() throws Exception {
        // ARRANGE
        LoginRequest request = new LoginRequest(
                "nobody@test.com",
                "password123"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_shouldFailWhenUserIsBlocked() throws Exception {
        // ARRANGE
        User user = createUser("blocked@test.com", "+79991234567");
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        LoginRequest request = new LoginRequest(
                "blocked@test.com",
                "password123"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==================== LOGOUT ====================

    @Test
    void logout_shouldRevokeToken() throws Exception {
        // ARRANGE
        createUser("logout@test.com", "+79991234567");
        String token = jwtService.generateToken("logout@test.com");

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertThat(revokedTokenRepository.existsByToken(token)).isTrue();
    }

    @Test
    void logout_shouldReturnAuthFailedWithoutToken() throws Exception {
        // ACT & ASSERT — /api/auth/** is permitAll, so returns 200 with failure message
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication Failed"));
    }

    @Test
    void tokenShouldNotWorkAfterLogout() throws Exception {
        // ARRANGE
        createUser("revoked@test.com", "+79991234567");
        String token = jwtService.generateToken("revoked@test.com");

        // logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // ACT & ASSERT — использовать revoked токен
        mockMvc.perform(post("/api/auth/send")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    // ==================== HELPERS ====================

    private User createUser(String email, String phone) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setConfirmed(true);
        return userRepository.save(user);
    }
}
