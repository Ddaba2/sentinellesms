package com.sentinellesms.controller;

import com.sentinellesms.dto.user.SuspendUserRequest;
import com.sentinellesms.dto.user.UpdateUserRolesRequest;
import com.sentinellesms.dto.user.UserResponse;
import com.sentinellesms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestParam(required = false) String q) {
        return userService.search(q);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable UUID id,
                                  @Valid @RequestBody SuspendUserRequest request,
                                  Authentication authentication) {
        return userService.setEnabled(id, request, authentication.getName());
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return userService.updateRoles(id, request);
    }
}
