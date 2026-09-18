package com.taskmanagement.mapper;

import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.dto.user.UserSummaryResponse;
import com.taskmanagement.entity.Role;
import com.taskmanagement.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEnabled(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserSummaryResponse toSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
