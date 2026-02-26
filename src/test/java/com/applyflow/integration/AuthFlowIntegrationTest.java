package com.applyflow.integration;

import com.applyflow.dto.LoginRequest;
import com.applyflow.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIntegrationTest extends BaseIntegrationTest {

        @Test
        @DisplayName("Should register a new user and return JWT token")
        void registerUser_Success() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .name("John Doe")
                                .email("john@example.com")
                                .password("password123")
                                .build();

                mockMvc.perform(post(AUTH_REGISTER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andExpect(jsonPath("$.name").value("John Doe"))
                                .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should reject duplicate email registration")
        void registerUser_DuplicateEmail() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .name("John Doe")
                                .email("dup@example.com")
                                .password("password123")
                                .build();

                mockMvc.perform(post(AUTH_REGISTER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post(AUTH_REGISTER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Email is already registered"));
        }

        @Test
        @DisplayName("Should reject registration with invalid data")
        void registerUser_ValidationError() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .name("")
                                .email("invalid-email")
                                .password("short")
                                .build();

                mockMvc.perform(post(AUTH_REGISTER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.validationErrors").isNotEmpty());
        }

        @Test
        @DisplayName("Should login with valid credentials and return JWT")
        void login_Success() throws Exception {
                registerAndGetToken("Jane Doe", "jane@example.com", "password123");

                LoginRequest loginRequest = LoginRequest.builder()
                                .email("jane@example.com")
                                .password("password123")
                                .build();

                mockMvc.perform(post(AUTH_LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andExpect(jsonPath("$.email").value("jane@example.com"));
        }

        @Test
        @DisplayName("Should reject login with wrong password")
        void login_InvalidCredentials() throws Exception {
                registerAndGetToken("Bob Smith", "bob@example.com", "password123");

                LoginRequest loginRequest = LoginRequest.builder()
                                .email("bob@example.com")
                                .password("wrongpassword")
                                .build();

                mockMvc.perform(post(AUTH_LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should access protected route with valid token")
        void accessProtectedRoute_WithToken() throws Exception {
                String token = registerAndGetToken();

                mockMvc.perform(get(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject access to protected route without token")
        void accessProtectedRoute_WithoutToken() throws Exception {
                mockMvc.perform(get(APPLICATIONS_URL))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject access with invalid JWT")
        void accessProtectedRoute_InvalidToken() throws Exception {
                // Use a properly-structured but unsigned/invalid JWT
                mockMvc.perform(get(APPLICATIONS_URL)
                                .header("Authorization",
                                                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlQGV4YW1wbGUuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.invalidsignature"))
                                .andExpect(status().isForbidden());
        }
}
