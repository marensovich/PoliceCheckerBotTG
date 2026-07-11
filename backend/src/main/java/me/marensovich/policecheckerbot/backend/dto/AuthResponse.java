package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful Telegram WebApp authentication.
 *
 * <p>Contains the session token and basic user information.
 * The token must be included in subsequent API requests as
 * {@code Authorization: Bearer <token>}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** UUID session token used for subsequent API authentication. */
    private String token;

    /** Telegram user ID. */
    private Long tgId;

    /** Telegram username (may be {@code null} if not set). */
    private String username;

    /** User's first name as provided by Telegram. */
    private String firstName;

    /** Whether the user has an active Premium subscription. */
    private Boolean isSubscribed;
}
