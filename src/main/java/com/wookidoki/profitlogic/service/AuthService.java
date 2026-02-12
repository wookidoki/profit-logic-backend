package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.DuplicateEmailException;
import com.wookidoki.profitlogic.common.exception.InvalidCredentialsException;
import com.wookidoki.profitlogic.config.jwt.JwtTokenProvider;
import com.wookidoki.profitlogic.domain.Role;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.auth.LoginRequest;
import com.wookidoki.profitlogic.dto.auth.LoginResponse;
import com.wookidoki.profitlogic.dto.auth.SignupRequest;
import com.wookidoki.profitlogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String roleStr = user.getRole() != null ? user.getRole().name() : Role.ROLE_USER.name();
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), roleStr);

        return LoginResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(roleStr)
                .build();
    }
}
