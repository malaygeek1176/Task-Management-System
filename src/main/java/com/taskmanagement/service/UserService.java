package com.taskmanagement.service;

import com.taskmanagement.dto.common.PageResponse;
import com.taskmanagement.dto.user.ChangePasswordRequest;
import com.taskmanagement.dto.user.UpdateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.security.UserPrincipal;

public interface UserService {

    UserResponse getById(Long id);

    PageResponse<UserResponse> getAll(int page, int size);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    UserResponse getCurrentUser(UserPrincipal principal);

    UserResponse updateCurrentUser(UserPrincipal principal, UpdateUserRequest request);

    void changePassword(UserPrincipal principal, ChangePasswordRequest request);
}
