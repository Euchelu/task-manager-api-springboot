package com.taskmanager.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.api.dto.request.LoginRequest;
import com.taskmanager.api.dto.request.RegisterRequest;
import com.taskmanager.api.dto.response.AuthResponse;
import com.taskmanager.api.dto.response.UserResponse;
import com.taskmanager.api.entity.Role;
import com.taskmanager.api.exception.DuplicateEmailException;
import com.taskmanager.api.exception.InvalidCredentialsException;
import com.taskmanager.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register returns 201 with token on success")
    void register_returns201_onSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Grace Hopper", "grace@example.com", "SuperSecret123");
        UserResponse userResponse = new UserResponse(1L, "Grace Hopper", "grace@example.com", Role.ROLE_USER);
        when(authService.register(any(RegisterRequest.class))).thenReturn(AuthResponse.of("jwt-token", userResponse));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("grace@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 400 when the payload is invalid")
    void register_returns400_whenPayloadInvalid() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 409 when the email is already registered")
    void register_returns409_whenEmailTaken() throws Exception {
        RegisterRequest request = new RegisterRequest("Grace Hopper", "grace@example.com", "SuperSecret123");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("An account with this email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with token on success")
    void login_returns200_onSuccess() throws Exception {
        LoginRequest request = new LoginRequest("grace@example.com", "SuperSecret123");
        UserResponse userResponse = new UserResponse(1L, "Grace Hopper", "grace@example.com", Role.ROLE_USER);
        when(authService.login(any(LoginRequest.class))).thenReturn(AuthResponse.of("jwt-token", userResponse));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 on invalid credentials")
    void login_returns401_onInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("grace@example.com", "wrong-password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
