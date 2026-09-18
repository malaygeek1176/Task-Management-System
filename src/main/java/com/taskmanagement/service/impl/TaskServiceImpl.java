package com.taskmanagement.service.impl;

import com.taskmanagement.dto.common.PageResponse;
import com.taskmanagement.dto.task.TaskAssignmentRequest;
import com.taskmanagement.dto.task.TaskCreateRequest;
import com.taskmanagement.dto.task.TaskPriorityUpdateRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.TaskStatusUpdateRequest;
import com.taskmanagement.dto.task.TaskUpdateRequest;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.TaskPriority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TaskSpecification;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TaskServiceImpl implements TaskService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, UserPrincipal principal) {
        User creator = findUserOrThrow(principal.getId());

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy(creator);

        if (request.getAssignedUserId() != null) {
            task.setAssignedUser(findUserOrThrow(request.getAssignedUserId()));
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireViewAccess(task, principal);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, Long assignedUserId,
                                                LocalDate dueDate, String search,
                                                int page, int size, String sortBy, String direction,
                                                UserPrincipal principal) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Specification<Task> spec = TaskSpecification.withFilters(status, priority, assignedUserId, dueDate, search);

        if (!isAdmin(principal)) {
            // Non-admins only ever see tasks they created or are assigned to.
            spec = spec.and(TaskSpecification.visibleTo(principal.getId()));
        }

        Page<Task> tasks = taskRepository.findAll(spec, pageRequest);
        return PageResponse.from(tasks.map(taskMapper::toResponse));
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdateRequest request, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireEditAccess(task, principal);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        task.setDueDate(request.getDueDate());

        if (request.getAssignedUserId() != null) {
            task.setAssignedUser(findUserOrThrow(request.getAssignedUserId()));
        } else {
            task.setAssignedUser(null);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void deleteTask(Long id, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireEditAccess(task, principal);
        taskRepository.delete(task);
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatusUpdateRequest request, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireStatusChangeAccess(task, principal);
        task.setStatus(request.getStatus());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse updatePriority(Long id, TaskPriorityUpdateRequest request, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireEditAccess(task, principal);
        task.setPriority(request.getPriority());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse assignTask(Long id, TaskAssignmentRequest request, UserPrincipal principal) {
        Task task = findTaskOrThrow(id);
        requireEditAccess(task, principal);
        task.setAssignedUser(findUserOrThrow(request.getAssignedUserId()));
        return taskMapper.toResponse(taskRepository.save(task));
    }

    // ---------------------------------------------------------------
    // Authorization helpers
    // ---------------------------------------------------------------

    /** Admin, the creator, or the assignee may view a task. */
    private void requireViewAccess(Task task, UserPrincipal principal) {
        if (isAdmin(principal) || isCreator(task, principal) || isAssignee(task, principal)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view this task");
    }

    /** Admin or the creator may edit, delete, re-prioritize, or reassign a task. */
    private void requireEditAccess(Task task, UserPrincipal principal) {
        if (isAdmin(principal) || isCreator(task, principal)) {
            return;
        }
        throw new AccessDeniedException("Only the task creator or an administrator can modify this task");
    }

    /** Admin, the creator, or the assignee may change a task's status. */
    private void requireStatusChangeAccess(Task task, UserPrincipal principal) {
        if (isAdmin(principal) || isCreator(task, principal) || isAssignee(task, principal)) {
            return;
        }
        throw new AccessDeniedException("Only the task creator, the assignee, or an administrator "
                + "can change this task's status");
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }

    private boolean isCreator(Task task, UserPrincipal principal) {
        return task.getCreatedBy() != null && task.getCreatedBy().getId().equals(principal.getId());
    }

    private boolean isAssignee(Task task, UserPrincipal principal) {
        return task.getAssignedUser() != null && task.getAssignedUser().getId().equals(principal.getId());
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Task", id));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("User", id));
    }
}
