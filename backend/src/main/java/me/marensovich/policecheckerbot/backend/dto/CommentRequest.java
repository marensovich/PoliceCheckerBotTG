package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for adding a comment to a DPS post.
 *
 * <p>Comment text must not be blank and may not exceed 300 characters.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class CommentRequest {

    @NotBlank(message = "Comment text must not be blank")
    @Size(max = 300, message = "Comment must not exceed 300 characters")
    private String text;
}
