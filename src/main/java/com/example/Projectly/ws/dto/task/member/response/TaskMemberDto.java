package com.example.Projectly.ws.dto.task.member.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.Projectly.bean.core.enums.TaskPriority;

/**
 * Response DTO for Task - Member view.
 * Visible to: MEMBER
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMemberDto {

    private Long id;
    private String ref;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String title;
    private String description;
    private TaskPriority priority;
    private Boolean completed;
    private LocalDate dueDate;
}
