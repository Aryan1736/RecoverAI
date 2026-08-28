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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(MerchantRepository merchantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public MerchantResponseDto register(MerchantRegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (merchantRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration failed: Email already exists: {}", normalizedEmail);
            throw new DuplicateMerchantException("Merchant with email '" + normalizedEmail + "' already exists");
        }

        if (request.getRazorpayAccountId() != null && !request.getRazorpayAccountId().isBlank()) {
            String razorpayAcc = request.getRazorpayAccountId().trim();
            if (merchantRepository.existsByRazorpayAccountId(razorpayAcc)) {
                log.warn("Registration failed: Razorpay account ID already exists: {}", razorpayAcc);
                throw new DuplicateMerchantException("Razorpay account ID '" + razorpayAcc + "' is already registered");
            }
        }

        String webhookSecret = request.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            webhookSecret = generateSecureWebhookSecret();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Merchant merchant = Merchant.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(encodedPassword)
                .razorpayAccountId(request.getRazorpayAccountId() != null ? request.getRazorpayAccountId().trim() : null)
                .webhookSecret(webhookSecret)
                .status(MerchantStatus.ACTIVE)
                .build();

        Merchant savedMerchant = merchantRepository.save(merchant);
        log.info("Successfully registered merchant id={}, email={}", savedMerchant.getId(), savedMerchant.getEmail());

        return MerchantResponseDto.fromEntity(savedMerchant);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(MerchantLoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Merchant merchant = merchantRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: Unknown email: {}", normalizedEmail);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (merchant.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), merchant.getPasswordHash())) {
            log.warn("Login failed: Invalid password for merchant id={}", merchant.getId());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            log.warn("Login failed: Merchant account not active, id={}, status={}", merchant.getId(), merchant.getStatus());
            throw new InvalidCredentialsException("Merchant account is not active");
        }

        String token = jwtTokenProvider.generateToken(merchant);
        log.info("Successfully authenticated merchant id={}, email={}", merchant.getId(), merchant.getEmail());

        return new AuthResponseDto(
                token,
                "Bearer",
                jwtProperties.getExpirationMs(),
                MerchantResponseDto.fromEntity(merchant)
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateSecureWebhookSecret() {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
