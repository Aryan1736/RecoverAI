package com.recoverai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.auth.MerchantLoginRequestDto;
import com.recoverai.backend.dto.auth.MerchantRegisterRequestDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "merchant_" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with valid payload should return 201 Created and safe merchant DTO")
    void testRegisterSuccess() throws Exception {
        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "Acme Retail",
                testEmail,
                "StrongPassword123!",
                "acc_test_" + UUID.randomUUID().toString().substring(0, 8),
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Acme Retail"))
                .andExpect(jsonPath("$.email").value(testEmail.toLowerCase()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.webhookSecret").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email should return 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        merchantRepository.save(Merchant.builder()
                .name("Existing Merchant")
                .email(testEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode("ExistingPass123!"))
                .status(MerchantStatus.ACTIVE)
                .build());

        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "Duplicate Merchant",
                testEmail,
                "StrongPassword123!",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with invalid data should return 400 Bad Request")
    void testRegisterInvalidData() throws Exception {
        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "", // invalid blank name
                "not-an-email", // invalid email
                "short", // too short password
                null,
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials should return 200 OK with Bearer token")
    void testLoginSuccess() throws Exception {
        merchantRepository.save(Merchant.builder()
                .name("Login Merchant")
                .email(testEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                .status(MerchantStatus.ACTIVE)
                .build());

        MerchantLoginRequestDto request = new MerchantLoginRequestDto(
                testEmail,
                "CorrectPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMs").isNumber())
                .andExpect(jsonPath("$.merchant.email").value(testEmail.toLowerCase()))
                .andExpect(jsonPath("$.merchant.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.merchant.webhookSecret").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with incorrect password should return 401 Unauthorized")
    void testLoginIncorrectPassword() throws Exception {
        merchantRepository.save(Merchant.builder()
                .name("Login Merchant")
                .email(testEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                .status(MerchantStatus.ACTIVE)
                .build());

        MerchantLoginRequestDto request = new MerchantLoginRequestDto(
                testEmail,
                "WrongPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with unknown email should return 401 Unauthorized")
    void testLoginUnknownEmail() throws Exception {
        MerchantLoginRequestDto request = new MerchantLoginRequestDto(
                "nonexistent@example.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
