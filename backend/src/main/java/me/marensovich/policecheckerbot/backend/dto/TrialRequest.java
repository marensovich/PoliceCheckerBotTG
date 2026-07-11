package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for granting a trial Premium period to a user.
 *
 * <p>The {@code days} value is added to the user's subscription expiry date
 * (or starts from now if not currently subscribed). Valid range: 1–365 days.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class TrialRequest {
    @NotNull
    @Min(1)
    @Max(365)
    private Integer days;
}
