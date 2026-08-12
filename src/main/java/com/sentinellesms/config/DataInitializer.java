package com.sentinellesms.config;

import com.sentinellesms.entity.Role;
import com.sentinellesms.entity.User;
import com.sentinellesms.repository.RoleRepository;
import com.sentinellesms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sentinellesms.admin.username}")
    private String adminUsername;

    @Value("${sentinellesms.admin.email}")
    private String adminEmail;

    @Value("${sentinellesms.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Role userRole = ensureRole(ROLE_USER);
        Role adminRole = ensureRole(ROLE_ADMIN);
        ensureAdminUser(userRole, adminRole);
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(null, name)));
    }

    private void ensureAdminUser(Role userRole, Role adminRole) {
        if (userRepository.existsByUsername(adminUsername) || userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setRoles(roles);

        userRepository.save(admin);
    }
}
