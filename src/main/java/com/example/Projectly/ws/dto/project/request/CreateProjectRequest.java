package com.example.Projectly.ws.dto.project.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import com.example.Projectly.bean.core.enums.ProjectStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "name is required")
    @Size(max = 500, message = "name must not exceed 500 characters")
    private String name;
    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
    private ProjectStatus status;
}

