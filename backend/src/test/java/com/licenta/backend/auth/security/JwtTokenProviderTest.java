package com.licenta.backend.auth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-tests-must-be-at-least-32-bytes-long";

    @Test
    void generatedTokenIsValidAndContainsEmail() {
        JwtTokenProvider jwt = new JwtTokenProvider(SECRET, 86_400_000L);

        String token = jwt.generateToken("user@example.com");

        assertThat(jwt.validate(token)).isTrue();
        assertThat(jwt.getEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void validateReturnsFalseForGarbageToken() {
        JwtTokenProvider jwt = new JwtTokenProvider(SECRET, 86_400_000L);

        assertThat(jwt.validate("not-a-valid-jwt")).isFalse();
    }

    @Test
    void validateReturnsFalseForExpiredToken() {
        JwtTokenProvider jwt = new JwtTokenProvider(SECRET, -1_000L);

        String expiredToken = jwt.generateToken("user@example.com");

        assertThat(jwt.validate(expiredToken)).isFalse();
    }
}
