package com.taskmanager.api.service;

import com.taskmanager.api.dto.request.LoginRequest;
import com.taskmanager.api.dto.request.RegisterRequest;
import com.taskmanager.api.dto.response.AuthResponse;
import com.taskmanager.api.entity.Role;
import com.taskmanager.api.entity.User;
import com.taskmanager.api.exception.DuplicateEmailException;
import com.taskmanager.api.exception.InvalidCredentialsException;
import com.taskmanager.api.repository.UserRepository;
import com.taskmanager.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Grace Hopper", "grace@example.com", "SuperSecret123");
        savedUser = User.builder()
                .id(1L)
                .fullName("Grace Hopper")
                .email("grace@example.com")
                .password("hashed-password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("registers a new user and returns a JWT")
    void register_createsUser_whenEmailNotTaken() {
        when(userRepository.existsByEmail("grace@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SuperSecret123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("grace@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("rejects registration when the email is already taken")
    void register_throwsDuplicateEmail_whenEmailExists() {
        when(userRepository.existsByEmail("grace@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("logs in a user with valid credentials and returns a JWT")
    void login_returnsToken_whenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest("grace@example.com", "SuperSecret123");
        when(userRepository.findByEmail("grace@example.com")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("grace@example.com");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("rejects login when credentials are invalid")
    void login_throwsInvalidCredentials_whenAuthenticationFails() {
        LoginRequest loginRequest = new LoginRequest("grace@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
