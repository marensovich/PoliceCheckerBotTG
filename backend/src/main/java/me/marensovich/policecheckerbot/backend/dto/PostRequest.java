package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import me.marensovich.policecheckerbot.backend.model.PostType;

/**
 * Request DTO for creating a new map marker (DPS post).
 *
 * <p>Latitude and longitude are required; all other fields are optional.
 * {@code patrolSpeedKmh} is only meaningful when {@code postType} is {@code PATROL_CAR}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class PostRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lon;

    @Size(max = 500)
    private String description;

    /** Post type. Defaults to {@code DPS_POST} if not specified. */
    private PostType postType = PostType.DPS_POST;

    /**
     * Patrol car speed in km/h.
     * Required only when {@code postType == PATROL_CAR}.
     */
    @Min(value = 0, message = "Speed cannot be negative")
    @Max(value = 300, message = "Speed cannot exceed 300 km/h")
    private Integer patrolSpeedKmh;
}
