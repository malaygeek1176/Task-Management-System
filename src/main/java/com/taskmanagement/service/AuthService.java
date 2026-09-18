package com.taskmanagement.service;

import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.LoginResponse;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.dto.user.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
