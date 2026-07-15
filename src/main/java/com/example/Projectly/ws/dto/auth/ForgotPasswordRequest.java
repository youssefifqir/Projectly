package com.example.Projectly.ws.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request a password reset email")
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    @Schema(description = "Email address of the account", example = "user@example.com")
    private String email;
}
