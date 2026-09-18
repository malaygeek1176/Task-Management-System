package com.taskmanagement.controller;

import com.taskmanagement.dto.common.ApiResponse;
import com.taskmanagement.dto.common.PageResponse;
import com.taskmanagement.dto.task.TaskAssignmentRequest;
import com.taskmanagement.dto.task.TaskCreateRequest;
import com.taskmanagement.dto.task.TaskPriorityUpdateRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.TaskStatusUpdateRequest;
import com.taskmanagement.dto.task.TaskUpdateRequest;
import com.taskmanagement.enums.TaskPriority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task creation, retrieval, updates, and assignment")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a task", description = "The authenticated user becomes the task's creator.")
    public ResponseEntity<ApiResponse<TaskResponse>> create(@Valid @RequestBody TaskCreateRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.createTask(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id",
            description = "Visible to admins, the task's creator, and the task's assignee.")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id, principal)));
    }

    @GetMapping
    @Operation(summary = "Search / filter / sort / paginate tasks",
            description = "Admins see every task; regular users only see tasks they created or are assigned to. "
                    + "Filters (status, priority, assignedUserId, dueDate, search) may be combined freely.")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAll(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        PageResponse<TaskResponse> response = taskService.getTasks(
                status, priority, assignedUserId, dueDate, search, page, size, sortBy, direction, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a task", description = "Only the task's creator or an admin may update it.")
    public ResponseEntity<ApiResponse<TaskResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody TaskUpdateRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.updateTask(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "Only the task's creator or an admin may delete it.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        taskService.deleteTask(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a task's status",
            description = "The task's creator, its assignee, or an admin may change status.")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody TaskStatusUpdateRequest request,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.updateStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", response));
    }

    @PatchMapping("/{id}/priority")
    @Operation(summary = "Change a task's priority", description = "Only the task's creator or an admin may change priority.")
    public ResponseEntity<ApiResponse<TaskResponse>> updatePriority(@PathVariable Long id,
                                                                      @Valid @RequestBody TaskPriorityUpdateRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.updatePriority(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Task priority updated successfully", response));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign a task to a user", description = "Only the task's creator or an admin may reassign it.")
    public ResponseEntity<ApiResponse<TaskResponse>> assign(@PathVariable Long id,
                                                              @Valid @RequestBody TaskAssignmentRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.assignTask(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Task assigned successfully", response));
    }
}
