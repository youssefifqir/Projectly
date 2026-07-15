package com.example.Projectly.ws.dto.task.member.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import com.example.Projectly.bean.core.enums.TaskPriority;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    private String title;
    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
    private TaskPriority priority;
    private Boolean completed;
    private LocalDate dueDate;
}
