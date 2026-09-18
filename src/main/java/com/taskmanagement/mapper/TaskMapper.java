package com.taskmanagement.mapper;

import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    private final UserMapper userMapper;

    public TaskMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueDate(task.getDueDate());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setAssignedUser(userMapper.toSummary(task.getAssignedUser()));
        response.setCreatedBy(userMapper.toSummary(task.getCreatedBy()));
        return response;
    }
}
