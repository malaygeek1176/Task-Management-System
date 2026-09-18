package com.taskmanagement.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.controller.TaskController;
import com.taskmanagement.dto.task.TaskAssignmentRequest;
import com.taskmanagement.dto.task.TaskCreateRequest;
import com.taskmanagement.dto.task.TaskPriorityUpdateRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.TaskStatusUpdateRequest;
import com.taskmanagement.dto.task.TaskUpdateRequest;
import com.taskmanagement.enums.TaskPriority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link TaskController}. Authorization for tasks is enforced
 * in the service layer (which is mocked here), so these tests focus on the
 * HTTP contract: request/response shapes and correct status-code mapping for
 * not-found / access-denied outcomes raised by the service.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private static UserPrincipal regularUser(long id) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UserPrincipal(id, "user" + id + "@example.com", "hash", true, authorities);
    }

    private static TaskResponse sampleTask(long id) {
        TaskResponse response = new TaskResponse();
        response.setId(id);
        response.setTitle("Sample task");
        response.setStatus(TaskStatus.TODO);
        response.setPriority(TaskPriority.MEDIUM);
        return response;
    }

    @Test
    void createTask_withValidRequest_returns201() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Write project report");
        request.setPriority(TaskPriority.HIGH);

        given(taskService.createTask(any(TaskCreateRequest.class), any())).willReturn(sampleTask(1L));

        mockMvc.perform(post("/api/tasks")
                        .with(user(regularUser(1L)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getTask_whenExists_returnsTask() throws Exception {
        given(taskService.getTask(eq(1L), any())).willReturn(sampleTask(1L));

        mockMvc.perform(get("/api/tasks/1").with(user(regularUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Sample task"));
    }

    @Test
    void getTask_whenMissing_returns404() throws Exception {
        given(taskService.getTask(eq(404L), any()))
                .willThrow(ResourceNotFoundException.forEntity("Task", 404L));

        mockMvc.perform(get("/api/tasks/404").with(user(regularUser(1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getTask_whenNotOwnedOrAssigned_returns403() throws Exception {
        given(taskService.getTask(eq(2L), any()))
                .willThrow(new AccessDeniedException("You do not have permission to view this task"));

        mockMvc.perform(get("/api/tasks/2").with(user(regularUser(1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void updateTask_withValidRequest_returnsUpdatedTask() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated title");

        given(taskService.updateTask(eq(1L), any(TaskUpdateRequest.class), any())).willReturn(sampleTask(1L));

        mockMvc.perform(put("/api/tasks/1")
                        .with(user(regularUser(1L)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteTask_returns200() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(user(regularUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_returnsUpdatedTask() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse updated = sampleTask(1L);
        updated.setStatus(TaskStatus.IN_PROGRESS);
        given(taskService.updateStatus(eq(1L), any(TaskStatusUpdateRequest.class), any())).willReturn(updated);

        mockMvc.perform(patch("/api/tasks/1/status")
                        .with(user(regularUser(1L)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void updatePriority_returnsUpdatedTask() throws Exception {
        TaskPriorityUpdateRequest request = new TaskPriorityUpdateRequest();
        request.setPriority(TaskPriority.LOW);

        TaskResponse updated = sampleTask(1L);
        updated.setPriority(TaskPriority.LOW);
        given(taskService.updatePriority(eq(1L), any(TaskPriorityUpdateRequest.class), any())).willReturn(updated);

        mockMvc.perform(patch("/api/tasks/1/priority")
                        .with(user(regularUser(1L)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("LOW"));
    }

    @Test
    void assignTask_returnsUpdatedTask() throws Exception {
        TaskAssignmentRequest request = new TaskAssignmentRequest();
        request.setAssignedUserId(2L);

        given(taskService.assignTask(eq(1L), any(TaskAssignmentRequest.class), any())).willReturn(sampleTask(1L));

        mockMvc.perform(patch("/api/tasks/1/assign")
                        .with(user(regularUser(1L)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
