package com.taskmanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.controller.UserController;
import com.taskmanagement.dto.common.PageResponse;
import com.taskmanagement.dto.user.UpdateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link UserController}. The servlet-level JWT filter is
 * disabled (that plumbing is exercised separately); instead each request is
 * given an already-authenticated {@link UserPrincipal} via
 * {@code SecurityMockMvcRequestPostProcessors.user(...)}. A minimal
 * {@code @EnableMethodSecurity} test configuration is imported so
 * {@code @PreAuthorize} is actually enforced against that principal.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.MethodSecurityTestConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static UserPrincipal regularUser(long id) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UserPrincipal(id, "user" + id + "@example.com", "hash", true, authorities);
    }

    private static UserPrincipal adminUser(long id) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new UserPrincipal(id, "admin" + id + "@example.com", "hash", true, authorities);
    }

    private static UserResponse sampleUser(long id) {
        return new UserResponse(id, "First", "Last", "user" + id + "@example.com",
                true, Set.of("USER"), null, null);
    }

    @Test
    void getCurrentUser_returnsOwnProfile() throws Exception {
        UserPrincipal principal = regularUser(1L);
        given(userService.getCurrentUser(any())).willReturn(sampleUser(1L));

        mockMvc.perform(get("/api/users/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateCurrentUser_returnsUpdatedProfile() throws Exception {
        UserPrincipal principal = regularUser(1L);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");

        given(userService.updateCurrentUser(any(), any(UpdateUserRequest.class))).willReturn(sampleUser(1L));

        mockMvc.perform(put("/api/users/me")
                        .with(user(principal))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_asAdmin_returnsUser() throws Exception {
        given(userService.getById(2L)).willReturn(sampleUser(2L));

        mockMvc.perform(get("/api/users/2").with(user(adminUser(99L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void getById_asRegularUserRequestingSomeoneElse_returns403() throws Exception {
        mockMvc.perform(get("/api/users/2").with(user(regularUser(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_asAdmin_returnsPagedUsers() throws Exception {
        PageResponse<UserResponse> page = new PageResponse<>(List.of(sampleUser(1L), sampleUser(2L)), 0, 10, 2, 1, true);
        given(userService.getAll(anyInt(), anyInt())).willReturn(page);

        mockMvc.perform(get("/api/users").with(user(adminUser(99L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void getAll_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users").with(user(regularUser(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/5")
                        .with(user(adminUser(99L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
