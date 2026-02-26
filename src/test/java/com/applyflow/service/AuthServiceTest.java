package com.applyflow.service;

import com.applyflow.dto.AuthResponse;
import com.applyflow.dto.LoginRequest;
import com.applyflow.dto.RefreshTokenRequest;
import com.applyflow.dto.RegisterRequest;
import com.applyflow.entity.RefreshToken;
import com.applyflow.entity.User;
import com.applyflow.enums.Role;
import com.applyflow.event.AuditEventPublisher;
import com.applyflow.exception.DuplicateResourceException;
import com.applyflow.exception.ResourceNotFoundException;
import com.applyflow.repository.RefreshTokenRepository;
import com.applyflow.repository.UserRepository;
import com.applyflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully with refresh token")
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getName()).isEqualTo("John Doe");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email exists")
    void register_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with refresh token")
    void login_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isNotNull().isNotEmpty();
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void login_InvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should refresh token successfully with rotation")
    void refreshToken_Success() {
        String rawToken = "test-raw-token";
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);
        when(jwtService.generateToken(any(User.class))).thenReturn("new-jwt-token");
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(rawToken)
                .build();

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-jwt-token");
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when refresh token is revoked (replay attack)")
    void refreshToken_RevokedToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("stolen-token")
                .build();

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void refreshToken_ExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should throw exception for non-existent refresh token")
    void refreshToken_NotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("non-existent")
                .build();

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should revoke all tokens on logout")
    void logout_Success() {
        authService.logout(user);

        verify(refreshTokenRepository).revokeAllByUser(user);
    }
}
