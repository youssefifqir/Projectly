package com.example.Projectly.config.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import org.springframework.stereotype.Component;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.email")
@Component
public class EmailProperties {

    private boolean enabled = true;

    @NotBlank
    @Email
    private String fromAddress;

    @NotBlank
    private String fromName;

    @Min(5)
    private long resetTokenExpiryMinutes = 30;

    @Min(1)
    private long verificationTokenExpiryHours = 24;

    private boolean verifyEmailOnRegister = false;
}
