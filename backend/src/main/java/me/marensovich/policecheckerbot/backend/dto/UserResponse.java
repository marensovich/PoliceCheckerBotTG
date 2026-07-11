package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.marensovich.policecheckerbot.backend.model.Role;

import java.time.LocalDateTime;

/**
 * Response DTO carrying the current user's profile.
 *
 * <p>Returned by {@code GET /api/users/me}. Contains all fields needed by the frontend
 * to render the profile page, subscription status, and notification settings.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private Long tgId;
    private String username;
    private String firstName;
    private Role role;
    private Integer reputationScore;
    private String notifyPostTypes;
    private Boolean isSubscribed;
    private LocalDateTime subscriptionExpiresAt;
    private Boolean isBanned;
    private Integer notifyRadiusKm;
    private Boolean liveTracking;
    private Integer promoDiscountPercent;
    private LocalDateTime createdAt;
}
