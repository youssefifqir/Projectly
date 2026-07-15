package com.example.Projectly.ws.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "User details as seen by an administrator")
public record AdminUserResponse(

        @Schema(description = "User UUID")
        String id,

        @Schema(description = "Unique ref")
        String ref,

        @Schema(description = "Email address")
        String email,

        @Schema(description = "First name")
        String firstName,

        @Schema(description = "Last name")
        String lastName,

        @Schema(description = "Assigned role names")
        List<String> roles,

        @Schema(description = "Whether the account is active")
        boolean enabled,

        @Schema(description = "Whether the account is locked")
        boolean locked,

        @Schema(description = "Whether the email has been verified")
        boolean emailVerified,

        @Schema(description = "Account creation timestamp")
        LocalDateTime createdDate,

        @Schema(description = "Last modification timestamp")
        LocalDateTime lastModifiedDate
) {}
