package com.taskmanagement.dto.task;

import com.taskmanagement.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;

    public TaskStatusUpdateRequest() {
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
