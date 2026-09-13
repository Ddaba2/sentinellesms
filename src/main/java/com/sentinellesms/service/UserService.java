package com.sentinellesms.service;

import com.sentinellesms.dto.user.SuspendUserRequest;
import com.sentinellesms.dto.user.UpdateUserRolesRequest;
import com.sentinellesms.dto.user.UserResponse;
import com.sentinellesms.entity.Role;
import com.sentinellesms.entity.User;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.exception.ResourceNotFoundException;
import com.sentinellesms.repository.RoleRepository;
import com.sentinellesms.repository.UserRepository;
import com.sentinellesms.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            Roles.SUPER_ADMIN, Roles.ADMIN, Roles.MODERATOR, Roles.ANALYST, Roles.USER
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public UserResponse getCurrentUser(String username) {
        return toResponse(findByUsername(username));
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return listUsers();
        }
        return userRepository.search(query.trim()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UserResponse setEnabled(UUID id, SuspendUserRequest request, String actorUsername) {
        User user = findById(id);
        if (user.getUsername().equals(actorUsername) && !request.isEnabled()) {
            throw new BadRequestException("Vous ne pouvez pas suspendre votre propre compte");
        }
        user.setEnabled(request.isEnabled());
        userRepository.save(user);
        auditService.log(request.isEnabled() ? "USER_ENABLE" : "USER_SUSPEND",
                "User", id.toString(), request.getReason());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, UpdateUserRolesRequest request) {
        User user = findById(id);
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            String normalized = normalizeRole(roleName);
            if (!ALLOWED_ROLES.contains(normalized)) {
                throw new BadRequestException("Rôle non autorisé: " + roleName);
            }
            Role role = roleRepository.findByName(normalized)
                    .orElseGet(() -> roleRepository.save(new Role(null, normalized)));
            roles.add(role);
        }
        if (roles.isEmpty()) {
            throw new BadRequestException("Au moins un rôle est requis");
        }
        user.setRoles(roles);
        userRepository.save(user);
        auditService.log("USER_ROLES_UPDATE", "User", id.toString(), String.join(",", request.getRoles()));
        return toResponse(user);
    }

    private String normalizeRole(String roleName) {
        String value = roleName.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value : "ROLE_" + value;
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + username));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
