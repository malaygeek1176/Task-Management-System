package com.taskmanagement.service;

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

import java.time.LocalDate;

public interface TaskService {

    TaskResponse createTask(TaskCreateRequest request, UserPrincipal principal);

    TaskResponse getTask(Long id, UserPrincipal principal);

    PageResponse<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, Long assignedUserId,
                                         LocalDate dueDate, String search,
                                         int page, int size, String sortBy, String direction,
                                         UserPrincipal principal);

    TaskResponse updateTask(Long id, TaskUpdateRequest request, UserPrincipal principal);

    void deleteTask(Long id, UserPrincipal principal);

    TaskResponse updateStatus(Long id, TaskStatusUpdateRequest request, UserPrincipal principal);

    TaskResponse updatePriority(Long id, TaskPriorityUpdateRequest request, UserPrincipal principal);

    TaskResponse assignTask(Long id, TaskAssignmentRequest request, UserPrincipal principal);
}
