package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for enabling or disabling live location tracking for the current user.
 *
 * <p>Live tracking is a Premium-only feature. Enabling it starts broadcasting the user's
 * position over WebSocket; disabling it stops the broadcast and clears the stored location.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class LiveTrackingRequest {

    /** {@code true} to enable live tracking, {@code false} to disable it. */
    @NotNull(message = "Field 'enabled' is required")
    private Boolean enabled;
}
