package com.wookidoki.profitlogic.config.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = "ZGV2LXNlY3JldC1rZXktZm9yLXByb2ZpdC1sb2dpYy1hcHBsaWNhdGlvbi0yMDI2";
        long expiration = 86400000L;
        jwtTokenProvider = new JwtTokenProvider(secret, expiration);
    }

    @Nested
    @DisplayName("토큰 생성")
    class CreateToken {

        @Test
        @DisplayName("유효한 토큰을 생성한다")
        void shouldCreateToken() {
            String token = jwtTokenProvider.createToken(1L, "test@example.com", "ROLE_USER");

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void shouldReturnTrueForValidToken() {
            String token = jwtTokenProvider.createToken(1L, "test@example.com", "ROLE_USER");

            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("잘못된 토큰은 false를 반환한다")
        void shouldReturnFalseForInvalidToken() {
            assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void shouldReturnFalseForEmptyToken() {
            assertFalse(jwtTokenProvider.validateToken(""));
        }
    }

    @Nested
    @DisplayName("토큰에서 정보 추출")
    class ExtractFromToken {

        @Test
        @DisplayName("토큰에서 userId를 추출한다")
        void shouldExtractUserId() {
            String token = jwtTokenProvider.createToken(42L, "test@example.com", "ROLE_USER");

            assertEquals(42L, jwtTokenProvider.getUserId(token));
        }

        @Test
        @DisplayName("토큰에서 email을 추출한다")
        void shouldExtractEmail() {
            String token = jwtTokenProvider.createToken(1L, "test@example.com", "ROLE_USER");

            assertEquals("test@example.com", jwtTokenProvider.getEmail(token));
        }

        @Test
        @DisplayName("토큰에서 role을 추출한다")
        void shouldExtractRole() {
            String token = jwtTokenProvider.createToken(1L, "test@example.com", "ROLE_ADMIN");

            assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token));
        }
    }
}
