package com.sentinellesms.service;

import com.sentinellesms.dto.auth.AuthResponse;
import com.sentinellesms.dto.auth.LoginRequest;
import com.sentinellesms.dto.auth.RegisterRequest;
import com.sentinellesms.entity.RefreshToken;
import com.sentinellesms.entity.Role;
import com.sentinellesms.entity.User;
import com.sentinellesms.exception.BadRequestException;
import com.sentinellesms.repository.RefreshTokenRepository;
import com.sentinellesms.repository.RoleRepository;
import com.sentinellesms.repository.UserRepository;
import com.sentinellesms.security.JwtService;
import com.sentinellesms.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Value("${sentinellesms.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        Role userRole = roleRepository.findByName(Roles.USER)
                .orElseGet(() -> roleRepository.save(new Role(null, Roles.USER)));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setRoles(roles);

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElse(null);

        if (user == null || !user.isEnabled()
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.log("LOGIN_FAILED", "User", user != null ? user.getId().toString() : null,
                    request.getUsernameOrEmail());
            throw new BadCredentialsException("Identifiants invalides");
        }

        AuthResponse response = buildAuthResponse(user);
        auditService.log("LOGIN_SUCCESS", "User", user.getId().toString(), user.getUsername());
        return response;
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token invalide"));

        if (stored.isRevoked() || stored.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expiré ou révoqué");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roleNames)
                .build();
    }

    private String issueRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
