package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for authenticating via Telegram WebApp.
 *
 * <p>Contains the {@code initData} string obtained from
 * {@code window.Telegram.WebApp.initData} on the frontend.
 * The backend validates the HMAC-SHA256 signature against the bot token.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 * @see me.marensovich.policecheckerbot.backend.security.TelegramAuthValidator
 */
@Data
public class AuthRequest {

    /** Signed Telegram WebApp init data string. */
    @NotBlank(message = "initData must not be blank")
    private String initData;
}
