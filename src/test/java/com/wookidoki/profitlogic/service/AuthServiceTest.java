package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.DuplicateEmailException;
import com.wookidoki.profitlogic.common.exception.InvalidCredentialsException;
import com.wookidoki.profitlogic.config.jwt.JwtTokenProvider;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.LoginRequest;
import com.wookidoki.profitlogic.dto.LoginResponse;
import com.wookidoki.profitlogic.dto.SignupRequest;
import com.wookidoki.profitlogic.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("정상: 새 이메일로 가입 성공")
        void shouldSignupSuccessfully() {
            SignupRequest request = SignupRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .nickname("테스터")
                    .build();

            given(userRepository.existsByEmail("test@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> authService.signup(request));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("예외: 중복 이메일 → DuplicateEmailException")
        void shouldThrowWhenEmailDuplicate() {
            SignupRequest request = SignupRequest.builder()
                    .email("dup@example.com")
                    .password("password123")
                    .nickname("테스터")
                    .build();

            given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

            assertThrows(DuplicateEmailException.class, () -> authService.signup(request));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상: 올바른 이메일/비번 → 토큰 반환")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("테스터")
                    .build();

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtTokenProvider.createToken(1L, "test@example.com")).willReturn("jwt-token");

            LoginResponse response = authService.login(request);

            assertEquals("jwt-token", response.getAccessToken());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("테스터", response.getNickname());
        }

        @Test
        @DisplayName("예외: 존재하지 않는 이메일 → InvalidCredentialsException")
        void shouldThrowWhenEmailNotFound() {
            LoginRequest request = LoginRequest.builder()
                    .email("wrong@example.com")
                    .password("password123")
                    .build();

            given(userRepository.findByEmail("wrong@example.com")).willReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("예외: 잘못된 비밀번호 → InvalidCredentialsException")
        void shouldThrowWhenPasswordWrong() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongpass")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("테스터")
                    .build();

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongpass", "encodedPassword")).willReturn(false);

            assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        }
    }
}
