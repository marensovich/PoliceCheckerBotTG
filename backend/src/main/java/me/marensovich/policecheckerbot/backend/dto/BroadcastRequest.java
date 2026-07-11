package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for broadcasting a message to a group of users via Telegram.
 *
 * <p>{@code target} controls who receives the broadcast:
 * {@code ALL} (default), {@code PREMIUM}, or {@code FREE}.
 * The message supports Telegram MarkdownV2 formatting.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class BroadcastRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;

    /** Recipient group: {@code ALL}, {@code PREMIUM}, or {@code FREE}. */
    private String target = "ALL";
}
