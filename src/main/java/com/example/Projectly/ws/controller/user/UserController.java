package com.example.Projectly.ws.controller.user;

import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.service.facade.security.UserService;
import com.example.Projectly.ws.dto.user.ChangePasswordRequest;
import com.example.Projectly.ws.dto.user.ProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @PatchMapping("/profile")
    @Operation(summary = "Update user profile information")
    public ResponseEntity<Void> updateProfile(
            @Valid @RequestBody final ProfileUpdateRequest request,
            @AuthenticationPrincipal final User user) {
        this.userService.updateProfileInfo(request, user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/password")
    @Operation(summary = "Change user password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody final ChangePasswordRequest request,
            @AuthenticationPrincipal final User user) {
        this.userService.changePassword(request, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate user account")
    public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal final User user) {
        this.userService.deactivateAccount(user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reactivate")
    @Operation(summary = "Reactivate user account")
    public ResponseEntity<Void> reactivateAccount(@AuthenticationPrincipal final User user) {
        this.userService.reactivateAccount(user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete user account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal final User user) {
        this.userService.deleteAccount(user.getId());
        return ResponseEntity.ok().build();
    }
}
