package me.marensovich.policecheckerbot.backend.dto;

import lombok.*;
import me.marensovich.policecheckerbot.backend.model.PostType;

import java.time.LocalDateTime;

/**
 * Response DTO representing a map marker (DPS post).
 *
 * <p>Returned by post listing and proximity search endpoints.
 * {@code distanceMeters} is populated only for proximity queries.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private PostType postType;
    private Double lat;
    private Double lon;
    private String description;
    private String addedByUsername;
    private Boolean isActive;

    /** Net confidence score (sum of all votes: {@code +1} confirm, {@code -1} fake). */
    private Integer confidence;

    /** Number of positive confirmation votes ({@code +1}). */
    private Integer confirmedCount;

    /** Patrol car speed in km/h (only present for {@code PATROL_CAR} posts). */
    private Integer patrolSpeedKmh;

    /** Expiry timestamp ({@code null} for permanent fixtures such as cameras). */
    private LocalDateTime expiresAt;

    /** Distance from the query point in metres (only present in proximity queries). */
    private Double distanceMeters;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentsCount;
}
