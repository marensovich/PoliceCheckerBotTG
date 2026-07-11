package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for an administrator's reply to a support ticket.
 *
 * <p>{@code status} is optional; if omitted, the ticket is automatically set to
 * {@code IN_PROGRESS}. Valid values: {@code OPEN}, {@code IN_PROGRESS}, {@code CLOSED}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class TicketReplyRequest {

    @NotBlank(message = "Reply must not be blank")
    @Size(max = 2000, message = "Reply must not exceed 2000 characters")
    private String reply;

    /** Optional new ticket status. If omitted the ticket is moved to {@code IN_PROGRESS}. */
    private String status;
}
