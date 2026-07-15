package com.example.Projectly.ws.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Reset password using a valid token")
public class ResetPasswordRequest {

    @NotBlank
    @Schema(description = "Token received via email")
    private String token;

    @NotBlank
    @Size(min = 8)
    @Schema(description = "New password (min 8 characters)")
    private String newPassword;

    @NotBlank
    @Schema(description = "Must match newPassword")
    private String confirmNewPassword;
}
