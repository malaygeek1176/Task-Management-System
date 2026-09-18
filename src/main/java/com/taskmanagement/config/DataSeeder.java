package com.taskmanagement.config;

import com.taskmanagement.entity.Role;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.RoleName;
import com.taskmanagement.repository.RoleRepository;
import com.taskmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Runs once on every application startup. Ensures the two system roles
 * (USER, ADMIN) exist and, optionally, that a development ADMIN account is
 * available. Every step is idempotent: nothing is duplicated on subsequent
 * restarts.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Value("${app.admin.seed-enabled:true}")
    private boolean adminSeedEnabled;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = seedRole(RoleName.USER);
        Role adminRole = seedRole(RoleName.ADMIN);

        if (adminSeedEnabled) {
            seedAdminAccount(userRole, adminRole);
        }
    }

    private Role seedRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Seeding missing role: {}", name);
                    return roleRepository.save(new Role(name));
                });
    }

    private void seedAdminAccount(Role userRole, Role adminRole) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        log.info("Seeding development admin account: {}", adminEmail);

        User admin = new User();
        admin.setFirstName(adminFirstName);
        admin.setLastName(adminLastName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setRoles(Set.of(userRole, adminRole));

        userRepository.save(admin);
    }
}
