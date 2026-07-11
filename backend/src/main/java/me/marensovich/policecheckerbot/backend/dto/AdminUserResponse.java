package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.marensovich.policecheckerbot.backend.model.Role;

import java.time.LocalDateTime;

/**
 * Extended user profile DTO for the admin panel.
 *
 * <p>Returned by user search and listing endpoints under {@code /api/admin/users}.
 * Includes moderator region, live tracking status, and last-seen timestamp in
 * addition to the standard user fields.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private Long tgId;
    private String username;
    private String firstName;
    private Role role;
    private String moderatorRegion;
    private Integer reputationScore;
    private Boolean isSubscribed;
    private LocalDateTime subscriptionExpiresAt;
    private Boolean isBanned;
    private Boolean liveTracking;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}
