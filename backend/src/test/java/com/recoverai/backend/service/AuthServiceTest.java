package com.recoverai.backend.service;

import com.recoverai.backend.config.JwtProperties;
import com.recoverai.backend.dto.auth.AuthResponseDto;
import com.recoverai.backend.dto.auth.MerchantLoginRequestDto;
import com.recoverai.backend.dto.auth.MerchantRegisterRequestDto;
import com.recoverai.backend.dto.auth.MerchantResponseDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.exception.DuplicateMerchantException;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private JwtProperties jwtProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties("test-secret-that-is-at-least-256-bits-long-for-testing", 3600000L, "RecoverAI");
        authService = new AuthService(merchantRepository, passwordEncoder, jwtTokenProvider, jwtProperties);
    }

    @Test
    @DisplayName("Should successfully register a new merchant with hashed password and active status")
    void testRegisterSuccess() {
        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "Acme Corp",
                "merchant@acme.com",
                "SecurePass123!",
                "acc_12345",
                null
        );

        when(merchantRepository.existsByEmail("merchant@acme.com")).thenReturn(false);
        when(merchantRepository.existsByRazorpayAccountId("acc_12345")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hashedPasswordHere");

        UUID generatedId = UUID.randomUUID();
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant m = invocation.getArgument(0);
            m.setId(generatedId);
            return m;
        });

        MerchantResponseDto response = authService.register(request);

        assertNotNull(response);
        assertEquals(generatedId, response.getId());
        assertEquals("Acme Corp", response.getName());
        assertEquals("merchant@acme.com", response.getEmail());
        assertEquals("acc_12345", response.getRazorpayAccountId());
        assertEquals(MerchantStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(captor.capture());
        Merchant saved = captor.getValue();
        assertEquals("$2a$10$hashedPasswordHere", saved.getPasswordHash());
        assertNotNull(saved.getWebhookSecret());
        assertTrue(saved.getWebhookSecret().startsWith("whsec_"));
    }

    @Test
    @DisplayName("Should throw DuplicateMerchantException if email is already registered")
    void testRegisterDuplicateEmail() {
        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "Acme Corp",
                "EXISTING@acme.com",
                "SecurePass123!",
                null,
                null
        );

        when(merchantRepository.existsByEmail("existing@acme.com")).thenReturn(true);

        DuplicateMerchantException ex = assertThrows(DuplicateMerchantException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("existing@acme.com"));
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateMerchantException if razorpay account ID is already registered")
    void testRegisterDuplicateRazorpayAccountId() {
        MerchantRegisterRequestDto request = new MerchantRegisterRequestDto(
                "Acme Corp",
                "new@acme.com",
                "SecurePass123!",
                "acc_duplicate",
                null
        );

        when(merchantRepository.existsByEmail("new@acme.com")).thenReturn(false);
        when(merchantRepository.existsByRazorpayAccountId("acc_duplicate")).thenReturn(true);

        DuplicateMerchantException ex = assertThrows(DuplicateMerchantException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("acc_duplicate"));
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void testLoginSuccess() {
        MerchantLoginRequestDto request = new MerchantLoginRequestDto("merchant@acme.com", "CorrectPassword123!");

        UUID merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("merchant@acme.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(MerchantStatus.ACTIVE)
                .build();

        when(merchantRepository.findByEmail("merchant@acme.com")).thenReturn(Optional.of(merchant));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(merchant)).thenReturn("mock.jwt.token");

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600000L, response.getExpiresInMs());
        assertNotNull(response.getMerchant());
        assertEquals(merchantId, response.getMerchant().getId());
        assertEquals("merchant@acme.com", response.getMerchant().getEmail());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email is not found")
    void testLoginUnknownEmail() {
        MerchantLoginRequestDto request = new MerchantLoginRequestDto("unknown@acme.com", "Password123!");

        when(merchantRepository.findByEmail("unknown@acme.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void testLoginWrongPassword() {
        MerchantLoginRequestDto request = new MerchantLoginRequestDto("merchant@acme.com", "WrongPassword!");

        Merchant merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("merchant@acme.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(MerchantStatus.ACTIVE)
                .build();

        when(merchantRepository.findByEmail("merchant@acme.com")).thenReturn(Optional.of(merchant));
        when(passwordEncoder.matches("WrongPassword!", "$2a$10$hashedPassword")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when merchant status is not ACTIVE")
    void testLoginInactiveMerchant() {
        MerchantLoginRequestDto request = new MerchantLoginRequestDto("merchant@acme.com", "CorrectPassword123!");

        Merchant merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("merchant@acme.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(MerchantStatus.SUSPENDED)
                .build();

        when(merchantRepository.findByEmail("merchant@acme.com")).thenReturn(Optional.of(merchant));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$10$hashedPassword")).thenReturn(true);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals("Merchant account is not active", ex.getMessage());
    }
}
