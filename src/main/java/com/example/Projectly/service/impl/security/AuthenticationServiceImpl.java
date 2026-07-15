package com.example.Projectly.service.impl.security;

import com.example.Projectly.bean.core.role.Role;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.config.security.TokenDenylist;
import com.example.Projectly.service.facade.email.PasswordResetService;
import com.example.Projectly.exception.BusinessException;
import com.example.Projectly.dao.facade.security.RoleDao;
import com.example.Projectly.dao.facade.security.UserDao;
import com.example.Projectly.service.facade.security.AuthenticationService;
import com.example.Projectly.service.facade.security.JwtService;
import com.example.Projectly.ws.converter.user.UserConverter;
import com.example.Projectly.ws.dto.auth.AuthenticationRequest;
import com.example.Projectly.ws.dto.auth.RefreshRequest;
import com.example.Projectly.ws.dto.auth.RegistrationRequest;
import com.example.Projectly.ws.dto.auth.AuthenticationResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.Projectly.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final TokenDenylist tokenDenylist;
    private final PasswordResetService passwordResetService;

    @Value("${app.security.jwt.remember-me-token-expiration:2592000000}")
    private long rememberMeTokenExpiration;

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse login(final AuthenticationRequest request) {
        final Authentication auth = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        final User user = (User) auth.getPrincipal();
        final String token = this.jwtService.generateAccessToken(user.getUsername());
        final String refreshToken = request.isRememberMe()
                ? this.jwtService.generateRefreshToken(user.getUsername(), this.rememberMeTokenExpiration)
                : this.jwtService.generateRefreshToken(user.getUsername());
        final String tokenType = "Bearer";
        return AuthenticationResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .build();
    }

    @Override
    @Transactional(timeout = 30)
    public void register(final RegistrationRequest request) {
        checkPasswords(request.getPassword(), request.getConfirmPassword());

        Role defaultRole = this.roleDao.findByName("ROLE_USER")
                .orElseGet(() -> this.roleDao.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("No roles configured in the system")));
        
        final List<Role> roles = new ArrayList<>();
        roles.add(defaultRole);

        final User user = this.userConverter.toUser(request);
        user.setRoles(roles);
        log.debug("Saving user {}", user);
        try {
            this.userDao.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            String constraint = extractConstraintName(e);
            if (constraint != null && constraint.toLowerCase().contains("email")) {
                throw new BusinessException(EMAIL_ALREADY_EXISTS);
            }
            throw new BusinessException(UNKNOWN_ERROR);
        }

        final List<User> users = new ArrayList<>();
        users.add(user);
        defaultRole.setUsers(users);

        this.roleDao.save(defaultRole);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(final RefreshRequest req) {
        final String newAccessToken = this.jwtService.refreshAccessToken(req.getRefreshToken());
        final String tokenType = "Bearer";
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(req.getRefreshToken())
                .tokenType(tokenType)
                .build();
    }

    @Override
    @Transactional(timeout = 30)
    public void logout(final String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return;
        final String token = authorizationHeader.substring(7);
        final String jti = this.jwtService.extractJti(token);
        if (jti != null) {
            final long ttl = this.jwtService.extractRemainingTtlSeconds(token);
            this.tokenDenylist.add(jti, ttl);
            log.debug("Token revoked: jti={}, ttl={}s", jti, ttl);
        }
    }

    private void checkPasswords(final String password, final String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(PASSWORD_MISMATCH);
        }
    }

    private String extractConstraintName(final DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                return ((ConstraintViolationException) cause).getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
