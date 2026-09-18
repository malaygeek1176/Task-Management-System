package com.taskmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.controller.AuthController;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.LoginResponse;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.exception.DuplicateResourceException;
import com.taskmanagement.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link AuthController}. Security filters are disabled since
 * both endpoints are public; only the request/response contract and error
 * mapping are under test here (business logic lives in {@code AuthService}).
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_withValidRequest_returns201AndUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password@123");

        UserResponse response = new UserResponse(1L, "John", "Doe", "john@example.com",
                true, Set.of("USER"), null, null);
        given(authService.register(any(RegisterRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password@123");

        given(authService.register(any(RegisterRequest.class)))
                .willThrow(new DuplicateResourceException("An account with email 'john@example.com' already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void register_withInvalidPayload_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("");
        request.setLastName("Doe");
        request.setEmail("not-an-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password@123");

        given(authService.login(any(LoginRequest.class))).willReturn(new LoginResponse("jwt-token", 86400000L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        given(authService.login(any(LoginRequest.class))).willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
