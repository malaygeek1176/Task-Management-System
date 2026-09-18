package com.taskmanagement.dto.task;

import com.taskmanagement.enums.TaskPriority;
import jakarta.validation.constraints.NotNull;

public class TaskPriorityUpdateRequest {

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    public TaskPriorityUpdateRequest() {
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
}
