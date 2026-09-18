package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link JpaSpecificationExecutor} lets the service layer combine any number
 * of optional filters (status, priority, assignedUser, dueDate, title search)
 * with pagination and sorting in a single, composable query.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
