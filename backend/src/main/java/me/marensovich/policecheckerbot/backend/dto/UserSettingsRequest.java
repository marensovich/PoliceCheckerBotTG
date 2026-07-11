package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request DTO for updating a user's notification settings.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class UserSettingsRequest {

    /** Notification radius in kilometres (1–50). */
    @Min(value = 1, message = "Minimum radius is 1 km")
    @Max(value = 50, message = "Maximum radius is 50 km")
    private Integer notifyRadiusKm;
}
