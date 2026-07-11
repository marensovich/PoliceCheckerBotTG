package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the "Nearby Posts" tab, including the caller's effective search radius.
 *
 * <p>Free users receive a smaller {@code maxRadiusKm} than Premium subscribers.
 * The frontend uses this value to display the radius badge.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyResponse {

    /** Maximum search radius applied to this request (km), determined by subscription tier. */
    private double maxRadiusKm;

    /** Whether the caller holds an active Premium subscription. */
    private boolean premium;

    /** Posts found within the radius, sorted by distance ascending. */
    private List<PostResponse> posts;
}
