package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import com.taskmanagement.enums.TaskPriority;
import com.taskmanagement.enums.TaskStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * Builds a composable {@link Specification} for the {@code GET /api/tasks}
 * endpoint so any combination of status / priority / assignedUser / dueDate /
 * search / visibility restriction can be applied at once.
 */
public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> withFilters(TaskStatus status,
                                                    TaskPriority priority,
                                                    Long assignedUserId,
                                                    LocalDate dueDate,
                                                    String search) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates = cb.and(predicates, cb.equal(root.get("priority"), priority));
            }
            if (assignedUserId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("assignedUser").get("id"), assignedUserId));
            }
            if (dueDate != null) {
                predicates = cb.and(predicates, cb.equal(root.get("dueDate"), dueDate));
            }
            if (StringUtils.hasText(search)) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            return predicates;
        };
    }

    /**
     * Restricts results to tasks visible to a non-admin user: tasks they
     * created OR tasks assigned to them.
     */
    public static Specification<Task> visibleTo(Long userId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("createdBy").get("id"), userId),
                cb.equal(root.get("assignedUser").get("id"), userId)
        );
    }
}
