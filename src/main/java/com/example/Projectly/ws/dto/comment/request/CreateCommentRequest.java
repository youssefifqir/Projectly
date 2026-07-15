package com.example.Projectly.ws.dto.comment.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "body is required")
    @Size(max = 500, message = "body must not exceed 500 characters")
    private String body;

    /** References an existing Task by id or ref. */
    private Long taskId;
    private String taskRef;
}

