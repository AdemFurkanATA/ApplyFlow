package com.applyflow.integration;

import com.applyflow.dto.AuthResponse;
import com.applyflow.dto.LoginRequest;
import com.applyflow.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String AUTH_REGISTER_URL = "/api/auth/register";
    protected static final String AUTH_LOGIN_URL = "/api/auth/login";
    protected static final String APPLICATIONS_URL = "/api/applications";

    protected static final String DEFAULT_NAME = "Test User";
    protected static final String DEFAULT_EMAIL = "test@example.com";
    protected static final String DEFAULT_PASSWORD = "password123";

    protected String registerAndGetToken() throws Exception {
        return registerAndGetToken(DEFAULT_NAME, DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    protected String registerAndGetToken(String name, String email, String password) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post(AUTH_REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post(AUTH_LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }
}
