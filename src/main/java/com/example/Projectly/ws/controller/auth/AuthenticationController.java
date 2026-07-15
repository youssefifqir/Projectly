package com.example.Projectly.ws.controller.auth;

import com.example.Projectly.service.facade.security.AuthenticationService;
import com.example.Projectly.ws.dto.auth.AuthenticationRequest;
import com.example.Projectly.ws.dto.auth.RefreshRequest;
import com.example.Projectly.ws.dto.auth.RegistrationRequest;
import com.example.Projectly.ws.dto.auth.AuthenticationResponse;
import com.example.Projectly.service.facade.email.PasswordResetService;
import com.example.Projectly.ws.dto.auth.ForgotPasswordRequest;
import com.example.Projectly.ws.dto.auth.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<Void> register(@Valid @RequestBody final RegistrationRequest request) {
        this.authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT tokens")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody final AuthenticationRequest request) {
        return ResponseEntity.ok(this.authenticationService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody final RefreshRequest request) {
        return ResponseEntity.ok(this.authenticationService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current access token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authHeader) {
        this.authenticationService.logout(authHeader);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        this.passwordResetService.initiateForgotPassword(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token received by email")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody final ResetPasswordRequest request) {
        this.passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmNewPassword());
        return ResponseEntity.noContent().build();
    }
}
