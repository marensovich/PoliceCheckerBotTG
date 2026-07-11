package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a DPS Tracker application user.
 *
 * <p>Users are identified by their unique Telegram ID ({@code tgId}).
 * Authentication is performed exclusively via Telegram WebApp {@code initData}.
 * The entity stores notification preferences, live-tracking state,
 * subscription details, and the active session token.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique Telegram user ID. Used as the primary external identifier. */
    @Column(name = "tg_id", nullable = false, unique = true)
    private Long tgId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "first_name", length = 100)
    private String firstName;

    /** User's role within the system. Defaults to {@code USER}; {@code ADMIN} is assigned manually. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /** Whether the user has an active Premium subscription. */
    @Column(name = "is_subscribed", nullable = false)
    @Builder.Default
    private Boolean isSubscribed = false;

    /** Timestamp when the user's Premium subscription expires. */
    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    /** Whether the user has been banned by an administrator. */
    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private Boolean isBanned = false;

    /** DPS post notification radius in kilometres. Default is 5 km. */
    @Column(name = "notify_radius_km", nullable = false)
    @Builder.Default
    private Integer notifyRadiusKm = 5;

    /** Whether live location tracking is currently active for this user. */
    @Column(name = "live_tracking", nullable = false)
    @Builder.Default
    private Boolean liveTracking = false;

    /** Last known latitude (populated while live tracking is active). */
    @Column(name = "last_lat")
    private Double lastLat;

    /** Last known longitude (populated while live tracking is active). */
    @Column(name = "last_lon")
    private Double lastLon;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    /**
     * Reputation score: incremented for each confirmed post, decremented for each fake report.
     * Displayed on the profile and used as a trust signal for map markers.
     */
    @Column(name = "reputation_score", nullable = false)
    @Builder.Default
    private Integer reputationScore = 0;

    /**
     * Comma-separated list of {@link PostType} values to notify about (e.g. {@code "DPS_POST,CAMERA"}).
     * {@code null} or empty string means "notify about all types".
     */
    @Column(name = "notify_post_types", length = 200)
    private String notifyPostTypes;

    /** Assigned moderation region (only relevant for {@link Role#MODERATOR} users). */
    @Column(name = "moderator_region", length = 100)
    private String moderatorRegion;

    /** Discount percentage on the next subscription purchase, granted via a promo code. */
    @Column(name = "promo_discount_percent")
    private Integer promoDiscountPercent;

    /** Active session token (UUID), issued upon successful Telegram WebApp authentication. */
    @Column(name = "session_token", length = 64)
    private String sessionToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
