package com.applyflow.service;

import com.applyflow.dto.AuthResponse;
import com.applyflow.dto.LoginRequest;
import com.applyflow.dto.RefreshTokenRequest;
import com.applyflow.dto.RegisterRequest;
import com.applyflow.entity.RefreshToken;
import com.applyflow.entity.User;
import com.applyflow.enums.AuditEventType;
import com.applyflow.enums.Role;
import com.applyflow.event.AuditEventPublisher;
import com.applyflow.exception.DuplicateResourceException;
import com.applyflow.exception.ResourceNotFoundException;
import com.applyflow.repository.RefreshTokenRepository;
import com.applyflow.repository.UserRepository;
import com.applyflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final EmailService emailService;
        private final AuditEventPublisher auditEventPublisher;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateResourceException("Email is already registered");
                }

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail().toLowerCase())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER)
                                .build();

                userRepository.save(user);
                emailService.sendWelcomeEmail(user.getEmail(), user.getName());
                auditEventPublisher.publish(AuditEventType.USER_REGISTERED, user.getId());

                String accessToken = jwtService.generateToken(user);
                String rawRefreshToken = generateAndPersistRefreshToken(user);

                log.info("User registered successfully: {}", user.getEmail());

                return AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(rawRefreshToken)
                                .name(user.getName())
                                .email(user.getEmail())
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail().toLowerCase(),
                                                request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                String token = jwtService.generateToken(user);
                auditEventPublisher.publish(AuditEventType.USER_LOGGED_IN, user.getId());
                String accessToken = jwtService.generateToken(user);
                String rawRefreshToken = generateAndPersistRefreshToken(user);

                log.info("User logged in successfully: {}", user.getEmail());

                return AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(rawRefreshToken)
                                .name(user.getName())
                                .email(user.getEmail())
                                .build();
        }

        @Transactional
        public AuthResponse refreshToken(RefreshTokenRequest request) {
                String tokenHash = hashToken(request.getRefreshToken());

                RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

                if (storedToken.isRevoked()) {
                        log.warn("Attempted reuse of revoked refresh token for user: {}",
                                        storedToken.getUser().getEmail());
                        refreshTokenRepository.revokeAllByUser(storedToken.getUser());
                        throw new IllegalStateException(
                                        "Refresh token has been revoked. All sessions invalidated for security.");
                }

                if (storedToken.isExpired()) {
                        storedToken.setRevoked(true);
                        refreshTokenRepository.save(storedToken);
                        throw new IllegalStateException("Refresh token has expired");
                }

                // Rotate: revoke old token, issue new one
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);

                User user = storedToken.getUser();
                String newAccessToken = jwtService.generateToken(user);
                String newRawRefreshToken = generateAndPersistRefreshToken(user);

                log.info("Token refreshed successfully for user: {}", user.getEmail());

                return AuthResponse.builder()
                                .token(newAccessToken)
                                .refreshToken(newRawRefreshToken)
                                .name(user.getName())
                                .email(user.getEmail())
                                .build();
        }

        @Transactional
        public void logout(User user) {
                refreshTokenRepository.revokeAllByUser(user);
                log.info("User logged out, all refresh tokens revoked: {}", user.getEmail());
        }

        private String generateAndPersistRefreshToken(User user) {
                String rawToken = UUID.randomUUID().toString();
                String tokenHash = hashToken(rawToken);

                Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshExpiration());

                RefreshToken refreshToken = RefreshToken.builder()
                                .tokenHash(tokenHash)
                                .user(user)
                                .expiresAt(expiresAt)
                                .revoked(false)
                                .build();

                refreshTokenRepository.save(refreshToken);
                return rawToken;
        }

        private String hashToken(String token) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
                        return Base64.getEncoder().encodeToString(hash);
                } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("SHA-256 algorithm not available", e);
                }
        }
}
