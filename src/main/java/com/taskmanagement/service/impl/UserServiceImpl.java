package com.taskmanagement.service.impl;

import com.taskmanagement.dto.common.PageResponse;
import com.taskmanagement.dto.user.ChangePasswordRequest;
import com.taskmanagement.dto.user.UpdateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return PageResponse.from(users.map(userMapper::toResponse));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        applyUpdate(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException.forEntity("User", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        return userMapper.toResponse(findUserOrThrow(principal.getId()));
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UserPrincipal principal, UpdateUserRequest request) {
        User user = findUserOrThrow(principal.getId());
        applyUpdate(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = findUserOrThrow(principal.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void applyUpdate(User user, UpdateUserRequest request) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("User", id));
    }
}
