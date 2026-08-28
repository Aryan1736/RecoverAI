package com.recoverai.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Password hash should differ from plaintext and never equal plaintext")
    void testPasswordHashDiffersFromPlaintext() {
        String rawPassword = "SecurePassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
    }

    @Test
    @DisplayName("Password encoder should verify correct password successfully")
    void testMatchesCorrectPassword() {
        String rawPassword = "CorrectPassword456$";
        String encoded = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("Password encoder should reject incorrect password")
    void testRejectsIncorrectPassword() {
        String rawPassword = "CorrectPassword456$";
        String wrongPassword = "WrongPassword789#";
        String encoded = passwordEncoder.encode(rawPassword);

        assertFalse(passwordEncoder.matches(wrongPassword, encoded));
    }

    @Test
    @DisplayName("Subsequent encodings of same password should produce different salted hashes")
    void testSaltedHashesAreUnique() {
        String rawPassword = "CommonPassword999@";
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(hash1, hash2);
        assertTrue(passwordEncoder.matches(rawPassword, hash1));
        assertTrue(passwordEncoder.matches(rawPassword, hash2));
    }
}
