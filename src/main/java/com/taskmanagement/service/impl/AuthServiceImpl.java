package com.taskmanagement.service.impl;

import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.LoginResponse;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.entity.Role;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.RoleName;
import com.taskmanagement.exception.DuplicateResourceException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.repository.RoleRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.JwtService;
import com.taskmanagement.security.UserPrincipal;
import com.taskmanagement.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            AuthenticationManager authenticationManager,
                            JwtService jwtService,
                            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with email '" + request.getEmail() + "' already exists");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured"));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Delegates credential checking to the configured AuthenticationProvider
        // (BCrypt comparison happens inside DaoAuthenticationProvider). Throws
        // BadCredentialsException on failure, which GlobalExceptionHandler maps to 401.
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new LoginResponse(token, jwtService.getExpirationMs());
    }
}
