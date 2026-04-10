package com.kreya.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kreya.auth.dto.LoginRequest;
import com.kreya.auth.dto.RegisterRequest;
import com.kreya.auth.dto.TokenResponse;
import com.kreya.auth.dto.VerifyEmailRequest;
import com.kreya.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void whenValidRequestThenReturnCreated() throws Exception{
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vasudev");
        request.setLastName("Kishan");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(false);

        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));

    }

    @Test
    void whenInvalidRequestThenReturnBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("");
        request.setFirstName("");
        request.setLastName("");
        request.setRegisteringAsSeller(null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenValidLoginRequestThenReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@kreya.com");
        request.setPassword("Password123!");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");
        tokenResponse.setTokenType("Bearer");

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void whenInvalidLoginRequestThenReturnBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenLoginFailsThenReturnUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@kreya.com");
        request.setPassword("WrongPassword123!");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void whenValidVerifyEmailRequestThenReturnOk() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("verification-token");

        doNothing().when(authService).verifyEmail(any(VerifyEmailRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Email verified successfully"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/verify-email"));
    }

    @Test
    void whenInvalidVerifyEmailRequestThenReturnBadRequest() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("");

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenVerifyEmailFailsThenReturnUnauthorized() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("bad-token");

        doThrow(new IllegalArgumentException("Invalid verification token"))
                .when(authService).verifyEmail(any(VerifyEmailRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid verification token"))
            .andExpect(jsonPath("$.path").value("/api/v1/auth/verify-email"));
    }

}
