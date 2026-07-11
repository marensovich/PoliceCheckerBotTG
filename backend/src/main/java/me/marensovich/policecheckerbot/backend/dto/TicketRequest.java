package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for submitting a new support ticket.
 *
 * <p>Subject must be 5–200 characters and message must be 10–2000 characters.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class TicketRequest {

    @NotBlank(message = "Subject must not be blank")
    @Size(min = 5, max = 200, message = "Subject must be 5–200 characters")
    private String subject;

    @NotBlank(message = "Message must not be blank")
    @Size(min = 10, max = 2000, message = "Message must be 10–2000 characters")
    private String message;
}
