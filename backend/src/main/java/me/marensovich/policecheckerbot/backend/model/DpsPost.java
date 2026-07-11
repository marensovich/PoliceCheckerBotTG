package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a map marker in DPS Tracker.
 *
 * <p>Supports multiple types (see {@link PostType}): stationary DPS checkpoints,
 * patrol cars, traffic cameras, ambushes, and hidden posts. Lifetime and
 * aging behaviour depend on the type:
 * <ul>
 *   <li>{@code DPS_POST / AMBUSH / HIDDEN_POST}: expires after 12 h, fades visually after 4 h</li>
 *   <li>{@code PATROL_CAR}: expires after 30 min; uncertainty circle grows with patrol speed</li>
 *   <li>{@code CAMERA}: permanent marker ({@code expiresAt} is {@code null})</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "dps_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DpsPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Type of this map marker. */
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    @Builder.Default
    private PostType postType = PostType.DPS_POST;

    /** Latitude (WGS-84). */
    @Column(name = "lat", nullable = false)
    private Double lat;

    /** Longitude (WGS-84). */
    @Column(name = "lon", nullable = false)
    private Double lon;

    /** Optional user-supplied description. */
    @Column(name = "description", length = 500)
    private String description;

    /** User who created this post. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    /** Whether the post is currently active. {@code false} when deactivated by time or low confidence. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Net confidence score: sum of all votes ({@code +1} confirm / {@code -1} fake). */
    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private Integer confidence = 0;

    /** Number of positive confirmation votes ({@code +1} only). Shown as a counter on the map marker. */
    @Column(name = "confirmed_count", nullable = false)
    @Builder.Default
    private Integer confirmedCount = 0;

    /**
     * Patrol car speed in km/h (only set for {@link PostType#PATROL_CAR} posts).
     * Used to calculate the expanding uncertainty circle radius on the map.
     */
    @Column(name = "patrol_speed_kmh")
    private Integer patrolSpeedKmh;

    /**
     * Expiry timestamp after which the post is automatically deactivated.
     * {@code null} for permanent markers ({@link PostType#CAMERA}).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
