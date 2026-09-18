package com.taskmanagement.dto.task;

import jakarta.validation.constraints.NotNull;

public class TaskAssignmentRequest {

    @NotNull(message = "assignedUserId is required")
    private Long assignedUserId;

    public TaskAssignmentRequest() {
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}
