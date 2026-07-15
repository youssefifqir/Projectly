package com.example.Projectly.ws.dto.auth;

import com.example.Projectly.common.validation.NonDisposableEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationRequest {

    @NotBlank(message = "VALIDATION.REGISTRATION.FIRST_NAME.BLANK")
    @Size(min = 1, max = 50, message = "VALIDATION.REGISTRATION.FIRST_NAME.SIZE")
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "VALIDATION.REGISTRATION.FIRST_NAME.PATTERN")
    @Schema(example = "John")
    private String firstName;

    @NotBlank(message = "VALIDATION.REGISTRATION.LAST_NAME.BLANK")
    @Size(min = 1, max = 50, message = "VALIDATION.REGISTRATION.LAST_NAME.SIZE")
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "VALIDATION.REGISTRATION.LAST_NAME.PATTERN")
    @Schema(example = "Smith")
    private String lastName;

    @NotBlank(message = "VALIDATION.REGISTRATION.EMAIL.NOT_BLANK")
    @Email(message = "VALIDATION.REGISTRATION.EMAIL.FORMAT")
    @NonDisposableEmail(message = "VALIDATION.REGISTRATION.EMAIL.DISPOSABLE")
    @Schema(example = "john@mail.com")
    private String email;

    @NotBlank(message = "VALIDATION.REGISTRATION.PASSWORD.NOT_BLANK")
    @Size(min = 8, max = 72, message = "VALIDATION.REGISTRATION.PASSWORD.SIZE")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*\\W).*$", 
            message = "VALIDATION.REGISTRATION.PASSWORD.WEAK")
    @Schema(example = "pAssword1!_")
    private String password;

    @NotBlank(message = "VALIDATION.REGISTRATION.CONFIRM_PASSWORD.NOT_BLANK")
    @Size(min = 8, max = 72, message = "VALIDATION.REGISTRATION.CONFIRM_PASSWORD.SIZE")
    @Schema(example = "pAssword1!_")
    private String confirmPassword;
}
