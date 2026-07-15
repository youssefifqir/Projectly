
package com.example.Projectly.ws.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileUpdateRequest {
    
    @Schema(example = "John")
    private String firstName;

    @Schema(example = "Smith")
    private String lastName;
}
